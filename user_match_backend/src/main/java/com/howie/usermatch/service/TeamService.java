package com.howie.usermatch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.howie.usermatch.model.domain.Team;
import com.howie.usermatch.model.domain.User;
import com.howie.usermatch.model.dto.TeamQuery;
import com.howie.usermatch.model.request.TeamJoinRequest;
import com.howie.usermatch.model.request.TeamQuitRequest;
import com.howie.usermatch.model.request.TeamUpdateRequest;
import com.howie.usermatch.model.vo.TeamUserVO;

import java.util.List;

/**
* @author 77194
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2026-08-18 18:32:29
*/
public interface TeamService extends IService<Team> {
    /**
     * 创建队伍
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍
     * @param teamQuery 查询条件
     * @param isAdmin 是否为管理员
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 分页搜索队伍（供 /team/list 无限滚动使用）
     *
     * @param teamQuery 查询条件
     * @param isAdmin 是否为管理员
     * @return 分页结果（含 total）
     */
    Page<TeamUserVO> listTeamsPage(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 删除队伍
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id, User loginUser);
}
