import random
import hashlib

random.seed(42)

TOTAL = 50000
START_ID = 600000
BATCH_SIZE = 500

SURNAMES = list("王李张刘陈杨赵黄周吴徐孙胡朱高林何郭马罗梁宋郑谢韩唐冯于董萧程曹袁邓许傅沈曾彭吕苏卢蒋蔡贾丁魏薛叶阎余潘杜戴夏钟汪田任姜范方石姚谭廖邹熊金陆郝孔白崔康毛邱秦江史顾侯邵孟龙万段漕雷钱")

GIVEN_CHARS = list("伟芳娜秀英敏静丽强磊洋勇艳杰娟涛明超秀兰霞平刚桂英华建华建国建军志强志明志国华文辉力斌鹏飞龙林玉婷玲桂萍红丹峰旭新星宇晨辰浩然睿博思雨欣怡雪琳瑶琪璐晨翔")

GIVEN_NAMES = [
    "伟", "芳", "娜", "秀英", "敏", "静", "丽", "强", "磊", "洋",
    "勇", "艳", "杰", "娟", "涛", "明", "超", "霞", "平", "刚",
    "华", "建", "文", "辉", "力", "斌", "鹏", "飞", "龙", "林",
    "婷", "玲", "红", "丹", "峰", "旭", "宇", "晨", "翔", "博",
    "思雨", "欣然", "怡雪", "琳瑶", "浩然", "子轩", "梓涵", "雨泽",
    "志强", "建华", "国强", "文杰", "海涛", "天宇", "子豪", "宇航",
    "晓明", "小红", "小明", "小刚", "小强", "小芳", "小丽", "小军",
    "建国", "建军", "国华", "志明", "海燕", "小红", "小芳", "小丽",
    "雪梅", "国华", "秀兰", "桂花", "玉兰", "淑芬", "淑珍", "美玲",
    "嘉欣", "佳琪", "子涵", "梓萱", "若曦", "诗涵", "梦瑶", "雨萱",
    "浩宇", "子墨", "一诺", "欣怡", "佳怡", "心怡", "美琳", "思琪",
]

TAG_POOL = [
    "Java", "Python", "C++", "Go", "Rust", "TypeScript", "JavaScript", "Swift", "Kotlin", "Ruby",
    "Spring", "Django", "React", "Vue", "Angular", "Flutter", "Docker", "Kubernetes",
    "MySQL", "Redis", "MongoDB", "PostgreSQL", "Elasticsearch", "Kafka", "RabbitMQ",
    "AI", "机器学习", "深度学习", "NLP", "计算机视觉", "大模型", "AIGC", "ChatGPT",
    "篮球", "足球", "乒乓球", "羽毛球", "网球", "游泳", "跑步", "健身", "瑜伽", "骑行",
    "跳舞", "街舞", "拉丁舞", "国标舞", "HipHop",
    "吉他", "钢琴", "小提琴", "架子鼓", "唱歌", "音乐", "摇滚", "民谣", "电子乐",
    "摄影", "旅行", "美食", "烹饪", "烘焙", "钓鱼", "登山", "露营", "徒步",
    "绘画", "书法", "动漫", "漫画", "Cosplay", "设计", "UI设计", "平面设计",
    "阅读", "写作", "诗歌", "科幻", "推理小说", "历史", "哲学", "心理学",
    "电竞", "LOL", "DOTA2", "CS2", "王者荣耀", "原神", "塞尔达", "独立游戏",
    "F1赛车", "卡丁车", "ChinaGT", "摩托车", "越野车",
    "电影", "追剧", "综艺", "纪录片", "短视频", "Vlog",
    "猫", "狗", "宠物", "多肉植物", "园艺",
    "区块链", "Web3", "前端", "后端", "全栈", "DevOps", "微服务", "云原生",
    "大数据", "Hadoop", "Spark", "数据分析师", "产品经理",
    "DeepSeek", "千问", "豆包", "Gemini", "Qoder", "agent",
]

PROFILES = [
    "热爱技术，喜欢折腾新东西",
    "生活不止眼前的代码，还有诗和远方",
    "终身学习者，每天都在进步",
    "代码改变世界",
    "保持好奇心，探索未知",
    "做一个有趣的人",
    "不积跬步无以至千里",
    "享受过程，结果顺其自然",
    "技术宅，但不社恐",
    "白天搬砖，晚上追梦",
    "人生苦短，我用Python",
    "Hello World!",
    "永远年轻，永远热泪盈眶",
    "码农一枚，请多关照",
    "在学新技术的路上从未停下",
    "喜欢交朋友，一起进步",
    "梦想是改变世界，先从bug改起",
    "平平无奇的小天才",
    "今天也要元气满满",
    "佛系青年，随缘写码",
    "努力成为更好的自己",
    "Stay hungry, stay foolish",
    "生命在于运动，编程在于实践",
    "爱生活，爱编程",
    "干饭人，干饭魂",
    "低调做人，高调写码",
    "认真生活，快乐编程",
    "每天进步一点点",
    "用代码书写人生",
    "路虽远行则将至，事虽难做则必成",
    "",
    "",
    "",
]

def gen_avatar(uid):
    return f"https://api.dicebear.com/7.x/avataaars/svg?seed=user{uid}"

PHONE_PREFIX = ["130", "131", "132", "133", "135", "136", "137", "138", "139",
                "150", "151", "152", "153", "155", "156", "157", "158", "159",
                "170", "171", "172", "173", "175", "176", "177", "178",
                "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
                "190", "191", "193", "195", "196", "197", "198", "199"]

EMAIL_DOMAINS = ["qq.com", "163.com", "126.com", "gmail.com", "outlook.com",
                 "foxmail.com", "sina.com", "aliyun.com", "hotmail.com", "yeah.net"]

PINYIN_PARTS = ["wang", "li", "zhang", "liu", "chen", "yang", "zhao", "huang", "zhou", "wu",
                "xu", "sun", "hu", "zhu", "gao", "lin", "he", "guo", "ma", "luo",
                "wei", "fang", "na", "jing", "li", "qiang", "lei", "yang", "yong", "yan",
                "jie", "tao", "ming", "chao", "xia", "ping", "gang", "hua", "wen", "hui",
                "peng", "fei", "long", "lin", "ting", "ling", "hong", "dan", "feng", "xu",
                "yu", "chen", "xiang", "bo", "si", "xin", "yi", "xue", "hao", "zi",
                "tian", "yu", "hang", "rui", "meng", "yao", "qi", "lu", "kai", "xin"]

PASSWORD = "3cf9868efa8a0d7d52fda24745c2bc40"


def gen_username():
    return random.choice(SURNAMES) + random.choice(GIVEN_NAMES)


def gen_tags():
    n = random.randint(3, 8)
    return random.sample(TAG_POOL, n)


def gen_profile():
    return random.choice(PROFILES)


def gen_gender():
    return random.choice([0, 1, 2])


def gen_phone():
    return random.choice(PHONE_PREFIX) + "".join([str(random.randint(0, 9)) for _ in range(8)])


def gen_email(uid):
    name = random.choice(PINYIN_PARTS) + random.choice(PINYIN_PARTS) + str(random.randint(1, 9999))
    domain = random.choice(EMAIL_DOMAINS)
    return f"{name}@{domain}"


def escape_sql(s):
    return s.replace("\\", "\\\\").replace("'", "\\'")


def main():
    out_path = r"D:\77194\Desktop\user_match_system\部署\fake_50k_users.sql"
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("SET NAMES utf8mb4;\n")
        f.write("SET @old_unique_checks = @@unique_checks, unique_checks = 0;\n")
        f.write("SET @old_fk_checks = @@foreign_key_checks, foreign_key_checks = 0;\n")
        f.write("SET @old_sql_mode = @@sql_mode;\n")
        f.write("SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';\n\n")

        f.write("START TRANSACTION;\n\n")

        f.write(f"DELETE FROM user_tag WHERE userId >= {START_ID} AND userId < {START_ID + TOTAL};\n")
        f.write(f"DELETE FROM user WHERE id >= {START_ID} AND id < {START_ID + TOTAL};\n\n")

        for batch_start in range(0, TOTAL, BATCH_SIZE):
            batch_end = min(batch_start + BATCH_SIZE, TOTAL)
            values = []
            for i in range(batch_start, batch_end):
                uid = START_ID + i
                username = escape_sql(gen_username())
                account = f"fake{i}"
                tags = gen_tags()
                tags_json = escape_sql('["' + '", "'.join(tags) + '"]')
                profile = escape_sql(gen_profile())
                gender = gen_gender()
                avatar = gen_avatar(uid)

                phone = gen_phone()
                email = gen_email(uid)

                values.append(
                    f"({uid},'{username}','{account}','{avatar}',{gender},"
                    f"'{PASSWORD}','{phone}','{email}','{tags_json}','{profile}')"
                )

            f.write(
                "INSERT INTO user (id, username, userAccount, avatarUrl, gender, "
                "userPassword, phone, email, tags, profile) VALUES\n"
            )
            f.write(",\n".join(values))
            f.write(";\n\n")

        f.write(
            f"INSERT INTO user_tag (userId, tag) "
            f"SELECT u.id, t.tag FROM user u "
            f"JOIN JSON_TABLE(u.tags, '$[*]' COLUMNS(tag VARCHAR(128) PATH '$')) t "
            f"WHERE u.id >= {START_ID} AND u.id < {START_ID + TOTAL};\n\n"
        )

        f.write("COMMIT;\n\n")
        f.write("SET unique_checks = @old_unique_checks;\n")
        f.write("SET foreign_key_checks = @old_fk_checks;\n")
        f.write("SET sql_mode = @old_sql_mode;\n")

    print(f"SQL file generated: {out_path}")
    print(f"Total users: {TOTAL}")
    print(f"ID range: {START_ID} ~ {START_ID + TOTAL - 1}")


if __name__ == "__main__":
    main()
