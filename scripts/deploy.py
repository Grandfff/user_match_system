#!/usr/bin/env python3
"""
LinkDot 一键部署脚本

流程：本地构建（后端 JAR + 前端 dist）-> SFTP 上传 -> 服务器上 docker compose 重建启动 -> 输出容器状态

服务器目录结构与仓库保持一致，避免“两份部署文件、改一处漏一处”：
    /opt/linkdot/
    ├── deploy/                  # docker-compose.yml / nginx / mysql 初始化脚本 / .env
    ├── user_match_backend/target/user_match_backend-0.0.1-SNAPSHOT.jar
    └── user_match_frontend/dist/

使用方式：
    pip install paramiko
    # 方式一：密钥登录（推荐）
    DEPLOY_HOST=1.2.3.4 DEPLOY_KEY=~/.ssh/id_rsa python scripts/deploy.py
    # 方式二：密码登录（运行后交互输入，不落盘、不入库）
    DEPLOY_HOST=1.2.3.4 python scripts/deploy.py

环境变量：
    DEPLOY_HOST  必填，服务器地址
    DEPLOY_USER  可选，默认 root
    DEPLOY_PORT  可选，默认 22
    DEPLOY_DIR   可选，默认 /opt/linkdot
    DEPLOY_KEY   可选，私钥路径；不填则交互输入密码
"""
import os
import sys
import getpass
import subprocess
from pathlib import Path

try:
    import paramiko
except ImportError:
    sys.exit("缺少依赖，请先执行: pip install paramiko")

PROJECT_ROOT = Path(__file__).resolve().parent.parent
FRONTEND_DIR = PROJECT_ROOT / "user_match_frontend"
BACKEND_DIR = PROJECT_ROOT / "user_match_backend"
DEPLOY_DIR = PROJECT_ROOT / "deploy"
JAR_NAME = "user_match_backend-0.0.1-SNAPSHOT.jar"

HOST = os.getenv("DEPLOY_HOST")
USER = os.getenv("DEPLOY_USER", "root")
PORT = int(os.getenv("DEPLOY_PORT", "22"))
REMOTE_DIR = os.getenv("DEPLOY_DIR", "/opt/linkdot")
KEY_PATH = os.getenv("DEPLOY_KEY")


def run_local(cmd, cwd=None):
    """执行本地命令，失败立即中断，避免带着半成品产物上服务器。"""
    print(f"[LOCAL] {cmd}")
    subprocess.run(cmd, cwd=cwd, shell=True, check=True)


def build_frontend() -> Path:
    print("\n=== 1/4 构建前端 ===")
    run_local("npm ci", cwd=FRONTEND_DIR)
    run_local("npm run build", cwd=FRONTEND_DIR)
    dist = FRONTEND_DIR / "dist"
    if not dist.exists():
        sys.exit("[ERROR] 前端构建失败：dist 目录不存在")
    print(f"[OK] 前端产物: {dist}")
    return dist


def build_backend() -> Path:
    print("\n=== 2/4 构建后端 ===")
    run_local("mvn -B -DskipTests package", cwd=BACKEND_DIR)
    jar = BACKEND_DIR / "target" / JAR_NAME
    if not jar.exists():
        sys.exit(f"[ERROR] 后端构建失败：{jar} 不存在")
    print(f"[OK] 后端产物: {jar}")
    return jar


def upload_tree(sftp, local_dir: Path, remote_dir: str):
    """递归上传目录，保持相对路径结构。"""
    for item in sorted(local_dir.rglob("*")):
        if not item.is_file():
            continue
        rel = item.relative_to(local_dir).as_posix()
        remote_path = f"{remote_dir}/{rel}"
        sftp.put(str(item), remote_path)
        print(f"  -> {remote_path}")


def remote_run(ssh, cmd: str):
    print(f"\n[REMOTE] {cmd}")
    _, stdout, stderr = ssh.exec_command(cmd)
    exit_code = stdout.channel.recv_exit_status()
    out, err = stdout.read().decode(), stderr.read().decode()
    if out:
        print(out.strip())
    if err:
        print(f"[STDERR] {err.strip()}")
    if exit_code != 0:
        sys.exit(f"[ERROR] 远程命令失败（exit={exit_code}）: {cmd}")


def main():
    if not HOST:
        sys.exit("请先设置服务器地址，例如：DEPLOY_HOST=1.2.3.4 python scripts/deploy.py")

    print("=" * 56)
    print(f"LinkDot 部署 -> {USER}@{HOST}:{REMOTE_DIR}")
    print("=" * 56)

    dist = build_frontend()
    jar = build_backend()

    print("\n=== 3/4 上传产物与编排文件 ===")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    connect_kwargs = {"hostname": HOST, "port": PORT, "username": USER, "timeout": 30}
    if KEY_PATH:
        connect_kwargs["key_filename"] = os.path.expanduser(KEY_PATH)
    else:
        connect_kwargs["password"] = getpass.getpass(f"请输入 {USER}@{HOST} 的登录密码: ")
    ssh.connect(**connect_kwargs)

    try:
        # 目录就绪（幂等，可重复执行）
        dirs = [
            f"{REMOTE_DIR}/deploy/nginx",
            f"{REMOTE_DIR}/deploy/mysql/init",
            f"{REMOTE_DIR}/deploy/backend",
            f"{REMOTE_DIR}/user_match_backend/target",
            f"{REMOTE_DIR}/user_match_frontend/dist",
        ]
        remote_run(ssh, f"mkdir -p {' '.join(dirs)}")

        with ssh.open_sftp() as sftp:
            sftp.put(str(jar), f"{REMOTE_DIR}/user_match_backend/target/{JAR_NAME}")
            print(f"  -> {REMOTE_DIR}/user_match_backend/target/{JAR_NAME}")

            upload_tree(sftp, dist, f"{REMOTE_DIR}/user_match_frontend/dist")

            for rel in ["docker-compose.yml", "nginx/nginx.conf",
                        "mysql/init/schema.sql", "backend/Dockerfile"]:
                sftp.put(str(DEPLOY_DIR / rel), f"{REMOTE_DIR}/deploy/{rel}")
                print(f"  -> {REMOTE_DIR}/deploy/{rel}")

            # 密钥文件只在本地存在时上传，避免覆盖服务器上已生效的凭证
            local_env = DEPLOY_DIR / ".env"
            if local_env.exists():
                sftp.put(str(local_env), f"{REMOTE_DIR}/deploy/.env")
                print(f"  -> {REMOTE_DIR}/deploy/.env（已更新）")
            else:
                print("[SKIP] 本地无 deploy/.env，沿用服务器上已有的 .env")

        print("\n=== 4/4 服务器端重建并启动 ===")
        remote_run(ssh, f"cd {REMOTE_DIR}/deploy && docker compose up -d --build")
        remote_run(ssh, f"cd {REMOTE_DIR}/deploy && sleep 15 && docker compose ps")
        remote_run(ssh, "docker image prune -f")
    finally:
        ssh.close()

    print("\n" + "=" * 56)
    print(f"部署完成，请访问 http://{HOST}")
    print("=" * 56)


if __name__ == "__main__":
    main()
