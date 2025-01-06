package top.lucki.lottery.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.lucki.lottery.common.api.BaseEntity;

@Data
@TableName("t_roster")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Roster extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
}
