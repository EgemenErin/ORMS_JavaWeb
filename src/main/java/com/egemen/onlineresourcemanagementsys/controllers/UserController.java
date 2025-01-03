package com.egemen.onlineresourcemanagementsys.controllers;

import com.egemen.onlineresourcemanagementsys.entities.User;
import com.egemen.onlineresourcemanagementsys.logic.AuthLogic;
import com.egemen.onlineresourcemanagementsys.logic.UserLogic;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class UserController {
    private final AuthLogic authLogic;
    private final UserLogic userLogic;

    public UserController(UserLogic userLogic, AuthLogic authLogic) {
        this.userLogic = userLogic;
        this.authLogic = authLogic;
    }

    @GetMapping("/users/login")
    public String login() {
        return "login";
    }

    @PostMapping("/users/logininfo")
    public String loginProcess(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               Model model, HttpSession session) {
        if (username.isBlank() || password.isBlank()) {
            model.addAttribute("error", "Username and password cannot be empty.");
            return "login";
        }

        try {
            User user = authLogic.loginUser(username, password);
            session.setAttribute("user", user);
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/users/register")
    public String register() {
        return "register";
    }

    @PostMapping("/users/registerProcess")
    public String registerProcess(@RequestParam("username") String username, @RequestParam("password") String password, Model model) {
        if (username.isBlank() || password.isBlank()) {
            model.addAttribute("error", "Username and password cannot be empty.");
            return "register";
        }

        try {
            userLogic.registerUser(username, password);
            return "redirect:/users/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @PostMapping("/users/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Clear the session
        return "redirect:/users/login"; // Redirect to login page
    }
}
