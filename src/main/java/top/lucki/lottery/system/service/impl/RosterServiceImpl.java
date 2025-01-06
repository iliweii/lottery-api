package top.lucki.lottery.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.lucki.lottery.system.entity.Roster;
import top.lucki.lottery.system.mapper.RosterMapper;
import top.lucki.lottery.system.service.IRosterService;
import top.lucki.lottery.system.vo.RosterVO;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class RosterServiceImpl extends ServiceImpl<RosterMapper, Roster> implements IRosterService {

    @Override
    public IPage<RosterVO> pageVO(Page<RosterVO> page, Wrapper<RosterVO> queryWrapper) {
        return this.baseMapper.selectPageVO(page, queryWrapper);
    }

    @Override
    public Roster getByUsername(String username) {
        return this.lambdaQuery().eq(Roster::getUsername, username).one();
    }
}
