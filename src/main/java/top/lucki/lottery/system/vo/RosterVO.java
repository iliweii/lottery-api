package top.lucki.lottery.system.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.lucki.lottery.system.entity.Roster;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class RosterVO extends Roster {
    private Integer status;
}
