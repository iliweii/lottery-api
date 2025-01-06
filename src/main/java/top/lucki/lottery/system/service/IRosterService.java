package top.lucki.lottery.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import top.lucki.lottery.system.entity.Roster;
import top.lucki.lottery.system.vo.RosterVO;

public interface IRosterService extends IService<Roster> {

    IPage<RosterVO> pageVO(Page<RosterVO> page, Wrapper<RosterVO> queryWrapper);

    Roster getByUsername(String username);

}
