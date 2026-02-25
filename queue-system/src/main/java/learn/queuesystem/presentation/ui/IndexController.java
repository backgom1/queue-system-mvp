package learn.queuesystem.presentation.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.UUID;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("userUuid", UUID.randomUUID().toString());
        return "index";
    }

    @GetMapping("/v2")
    public String indexV2(Model model) {
        model.addAttribute("userUuid", UUID.randomUUID().toString());
        return "index2";
    }

    @GetMapping("/monitor")
    public String monitor() {
        return "monitor";
    }
}
