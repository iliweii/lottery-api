package top.lucki.lottery.message.template;

import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.Map;

public class MailResultTemplate {

    public static String get(String line1, String line2) {
        Map<String, String> map = new HashMap<>();
        map.put("line1", line1);
        map.put("line2", line2);
        return StrUtil.format("<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "</head>\n" +
                "\n" +
                "<body>\n" +
                "    <style>\n" +
                "        * {\n" +
                "            /* 采用怪异模式下的盒模型：元素的总高度和宽度包含内边距和边框(padding与border)  */\n" +
                "            box-sizing: border-box;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "\n" +
                "        body {\n" +
                "            /* 没有滚动条 */\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "\n" +
                "        .section {\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            position: relative;\n" +
                "            min-height: 100vh;\n" +
                "            background: linear-gradient(135deg, #111, #222, #111);\n" +
                "        }\n" +
                "\n" +
                "        .section::before {\n" +
                "            content: \"\";\n" +
                "            position: absolute;\n" +
                "            width: 30vw;\n" +
                "            height: 30vw;\n" +
                "            /* 红色边框 */\n" +
                "            border: 5vw solid #ff1062;\n" +
                "            /* 圆形边框 */\n" +
                "            border-radius: 50%;\n" +
                "            /* 为边框添加2个下拉阴影 */\n" +
                "            box-shadow: 0 0 0 1vw #222, 0 0 0 1.3vw #fff;\n" +
                "        }\n" +
                "\n" +
                "        .section .section__title {\n" +
                "            position: absolute;\n" +
                "            transform: skewY(-7deg);\n" +
                "            z-index: 10;\n" +
                "            color: #fff;\n" +
                "            text-align: center;\n" +
                "            font-size: 9vw;\n" +
                "            line-height: 2em;\n" +
                "            text-shadow: 1px 1px 0 #ccc, 2px 2px 0 #ccc, 3px 3px 0 #ccc, 4px 4px 0 #ccc,\n" +
                "                5px 5px 0 #ccc, 10px 10px 0 rgba(0, 0, 0, 0.2);\n" +
                "            animation: floating 5s ease-in-out infinite;\n" +
                "        }\n" +
                "\n" +
                "        .section .section__title span {\n" +
                "            text-shadow: 1px 1px 0 #ccc, 2px 2px 0 #ccc, 3px 3px 0 #ccc, 4px 4px 0 #ccc,\n" +
                "                5px 5px 0 #ccc, 6px 6px 0 #ccc, 7px 7px 0 #ccc, 8px 8px 0 #ccc,\n" +
                "                9px 9px 0 #ccc, 20px 20px 0 rgba(0, 0, 0, 0.2);\n" +
                "            font-weight: 700;\n" +
                "            font-size: 2em;\n" +
                "        }\n" +
                "\n" +
                "        .section i {\n" +
                "            position: absolute;\n" +
                "            background: #fff;\n" +
                "            border-radius: 50%;\n" +
                "            box-shadow: 0 0 10px #fff, 0 0 20px #fff, 0 0 40px #fff, 0 0 80px #fff;\n" +
                "            animation: animate linear infinite;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes floating {\n" +
                "\n" +
                "            0%,\n" +
                "            100% {\n" +
                "                transform: skewY(-7deg) translate(0, -20px);\n" +
                "            }\n" +
                "\n" +
                "            50% {\n" +
                "                transform: skewY(-7deg) translate(0, 20px);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* 利用透明度设置星星明暗变化的动画效果 */\n" +
                "        @keyframes animate {\n" +
                "            0% {\n" +
                "                opacity: 0;\n" +
                "            }\n" +
                "\n" +
                "            10% {\n" +
                "                opacity: 1;\n" +
                "            }\n" +
                "\n" +
                "            90% {\n" +
                "                opacity: 1;\n" +
                "            }\n" +
                "\n" +
                "            100% {\n" +
                "                opacity: 0;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "    <section class=\"section\">\n" +
                "        <h2 class=\"section__title\">{line1}<br /><span>{line2}</span></h2>\n" +
                "    </section>\n" +
                "    <script>\n" +
                "        const stars = () => {\n" +
                "            const count = 200;\n" +
                "            const section = document.querySelector('.section');\n" +
                "            let i = 0;\n" +
                "            while (i < count) {\n" +
                "                // 在内存中创建一个新的空元素对象,如i或是div\n" +
                "                const star = document.createElement('i');\n" +
                "                // 定义变量x和y ：通过Math.random()方法随机的使星星出现在不同位置，当然星星的定位要在文档显示区内\n" +
                "                const x = Math.floor(Math.random() * window.innerWidth);\n" +
                "                const y = Math.floor(Math.random() * window.innerHeight);\n" +
                "                const size = Math.random() * 4;\n" +
                "                // 让星星始终会在网页最左最顶端出现，通过想x和y的定位，我们要让它出现在页面各个不同的位置\n" +
                "                star.style.left = x + 'px';\n" +
                "                star.style.top = y + 'px';\n" +
                "                // 利用Math.random()这个方法来随机取星星的大小：为每颗星星设置随机的宽高范围为[0,5)\n" +
                "                star.style.width = 1 + size + 'px';\n" +
                "                star.style.height = 1 + size + 'px';\n" +
                "\n" +
                "                const duration = Math.random() * 2;\n" +
                "\n" +
                "                // 设置持续时间\n" +
                "                // js中除了减法计算之外，不允许随便写-。因为会混淆。所以，DOM标准规定，所有带-的css属性名，一律去横线变驼峰\n" +
                "                // css属性animation-duration,在js中改写为驼峰形式：animationDuration\n" +
                "                star.style.animationDuration = 2 + duration + 's';\n" +
                "                // 设置延迟 \n" +
                "                star.style.animationDelay = 2 + duration + 's';\n" +
                "                // 将新元素添加到DOM树：把新创建的节点追加到父元素下所有直接子元素的结尾\n" +
                "                section.appendChild(star);\n" +
                "                i++;\n" +
                "            }\n" +
                "        }\n" +
                "        // 调用函数\n" +
                "        stars();\n" +
                "    </script>\n" +
                "</body>\n" +
                "\n" +
                "</html>", map);
    }
}
