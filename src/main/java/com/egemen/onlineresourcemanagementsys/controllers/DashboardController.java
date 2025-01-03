package com.egemen.onlineresourcemanagementsys.controllers;

import com.egemen.onlineresourcemanagementsys.entities.GameAccount;
import com.egemen.onlineresourcemanagementsys.entities.Resource;
import com.egemen.onlineresourcemanagementsys.entities.Subscription;
import com.egemen.onlineresourcemanagementsys.entities.User;
import com.egemen.onlineresourcemanagementsys.logic.ResourceLogic;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {
    private final ResourceLogic resourceLogic;

    public DashboardController(ResourceLogic resourceLogic) {
        this.resourceLogic = resourceLogic;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Object userObj = session.getAttribute("user");
        if (userObj == null) {
            return "redirect:/users/login"; // Redirect to login if no user is logged in
        }
        User user = (User) userObj;
        model.addAttribute("username", user.getUsername().toUpperCase());
        Map<String, List<Resource>> groupedResources = resourceLogic.groupResourcesByType(user.getId());
        model.addAttribute("groupedResources", groupedResources);
        return "dashboard";
    }


    @GetMapping("/resources")
    public String viewResources(HttpSession session, Model model) {
        Object loggedInUser = session.getAttribute("user");
        if (loggedInUser == null) {
            return "redirect:/users/login";
        }
        User user = (User) loggedInUser;

        Map<String, List<Resource>> groupedResources = resourceLogic.groupResourcesByType(user.getId());

        String username = user.getUsername().toUpperCase();
        model.addAttribute("groupedResources", groupedResources);
        model.addAttribute("username", username);
        return "viewResources";
    }

    @GetMapping("/resources/delete/{id}")
    public String deleteResource(@PathVariable int id) {
        resourceLogic.deleteResource(id);
        return "redirect:/resources";
    }

    @GetMapping("/resources/edit/{id}")
    public String editResourceForm(@PathVariable int id, Model model) {
        Resource resource = resourceLogic.findResourceById(id);
        if (resource == null) {
            model.addAttribute("error", "Resource not found");
            return "redirect:/resources";
        }
        model.addAttribute("resource", resource);
        return "editResource";
    }

    @PostMapping("/resources/edit/{id}")
    public String editResource(
            @PathVariable("id") int id,
            @RequestParam("name") String name,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "gamePlatform", required = false) String gamePlatform,
            @RequestParam(value = "subscriptionType", required = false) String subscriptionType,
            Model model
    ) {
        try {
            // Retrieve the resource
            Resource resource = resourceLogic.findResourceById(id);
            if (resource == null) {
                model.addAttribute("error", "Resource not found.");
                return "editResource";
            }

            // Update common fields
            resource.setName(name);
            resource.setUsername(username);
            resource.setPassword(password);

            // Update specific fields based on the resource type
            if (resource instanceof GameAccount gameAccount) {
                if (gamePlatform == null || gamePlatform.isBlank()) {
                    model.addAttribute("error", "Game Platform is required for Game Accounts.");
                    return "editResource";
                }
                gameAccount.setGamePlatform(gamePlatform);
            } else if (resource instanceof Subscription subscription) {
                if (subscriptionType == null || subscriptionType.isBlank()) {
                    model.addAttribute("error", "Subscription Type is required for Subscriptions.");
                    return "editResource";
                }
                subscription.setSubscriptionType(subscriptionType);
            }

            // Save the updated resource
            resourceLogic.saveResource(resource);

            // Redirect to the resource list with a success message
            model.addAttribute("success", "Resource updated successfully!");
            return "redirect:/resources";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while updating the resource: " + e.getMessage());
            return "editResource";
        }
    }


    @GetMapping("/resources/add-resource")
    public String addResourceForm() {
        return "addResource";
    }

    @PostMapping("/resources/add-resource")
    public String addResource(@RequestParam String name,
                              @RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String resourceType,
                              @RequestParam(value = "gamePlatform", required = false) String gamePlatform,
                              @RequestParam(value = "subscriptionType", required = false) String subscriptionType,
                              HttpSession session,
                              Model model) {
        Object loggedInUser = session.getAttribute("user");
        if (loggedInUser == null) {
            model.addAttribute("error", "User session expired. Please log in again.");
            return "redirect:/users/login";
        }

        User user = (User) loggedInUser;

        try {
            Resource resource;
            if ("GameAccount".equals(resourceType)) {
                if (gamePlatform == null || gamePlatform.isBlank()) {
                    model.addAttribute("error", "Game Platform is required for Game Accounts.");
                    return "addResource";
                }
                resource = new GameAccount(name, username, password, gamePlatform);
            } else if ("Subscription".equals(resourceType)) {
                if (subscriptionType == null || subscriptionType.isBlank()) {
                    model.addAttribute("error", "Subscription Type is required for Subscriptions.");
                    return "addResource";
                }
                resource = new Subscription(name, username, password, subscriptionType);
            } else {
                model.addAttribute("error", "Invalid resource type selected.");
                return "addResource";
            }

            resourceLogic.addResource(resource, user);
            return "redirect:/resources";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while adding the resource: " + e.getMessage());
            return "addResource";
        }
    }
}
