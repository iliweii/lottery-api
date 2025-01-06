package top.lucki.lottery.ws;

import cn.hutool.core.date.DateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.lucki.lottery.common.api.BaseEntity;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class WsMessage extends BaseEntity {
    private String type; // 类型 倒计时/中奖/聊天消息（文字、图片等等）
    private String message;
    private DateTime time;

    public WsMessage(String type, String message) {
        this.type = type;
        this.message = message;
        this.time = DateTime.now();
    }
}
