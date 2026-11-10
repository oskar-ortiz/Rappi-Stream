package com.rappistream.dashboardbackend.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardPageController {
    @GetMapping("/")
    public String dashboard() {
        return "forward:/dashboard.html";
    }
}
