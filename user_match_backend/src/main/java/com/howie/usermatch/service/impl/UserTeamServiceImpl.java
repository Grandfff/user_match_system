package com.howie.usermatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.howie.usermatch.mapper.UserTeamMapper;
import com.howie.usermatch.model.domain.UserTeam;
import com.howie.usermatch.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author 77194
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2026-08-18 16:50:57
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




