package top.lucki.lottery.system.mapper;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import top.lucki.lottery.system.entity.Roster;
import top.lucki.lottery.system.vo.RosterVO;

public interface RosterMapper extends BaseMapper<Roster> {

    IPage<RosterVO> selectPageVO(Page<RosterVO> page, @Param("ew") Wrapper<RosterVO> queryWrapper);

}
