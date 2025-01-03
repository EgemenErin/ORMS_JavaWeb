package com.egemen.onlineresourcemanagementsys.controllers;

import com.egemen.onlineresourcemanagementsys.entities.GameAccount;
import com.egemen.onlineresourcemanagementsys.entities.Resource;
import com.egemen.onlineresourcemanagementsys.entities.Subscription;
import com.egemen.onlineresourcemanagementsys.entities.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final RestTemplate restTemplate;

    @Autowired
    public DashboardController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static final String AUTH_SERVICE_URL = "http://192.168.0.18:1546";
    private static final String RESOURCE_SERVICE_URL = "http://192.168.0.18:1547";

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Object userObj = session.getAttribute("user");
        if (userObj == null) {
            return "redirect:/users/login"; // Redirect to login if no user is logged in
        }
        User user = (User) userObj;
        model.addAttribute("username", user.getUsername().toUpperCase());

        // Fetch resources using ResourceService
        String resourceServiceUrl = RESOURCE_SERVICE_URL + "/resources/user/" + user.getId();
        ResponseEntity<Resource[]> response = restTemplate.getForEntity(resourceServiceUrl, Resource[].class);
        List<Resource> resources = Arrays.asList(response.getBody());

        // Group resources by type
        Map<String, List<Resource>> groupedResources = resources.stream()
                .collect(Collectors.groupingBy(resource -> resource.getClass().getSimpleName()));

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

        // Fetch resources using ResourceService
        String resourceServiceUrl = RESOURCE_SERVICE_URL + "/resources/user/" + user.getId();
        ResponseEntity<Resource[]> response = restTemplate.getForEntity(resourceServiceUrl, Resource[].class);
        List<Resource> resources = Arrays.asList(response.getBody());

        // Group resources by type
        Map<String, List<Resource>> groupedResources = resources.stream()
                .collect(Collectors.groupingBy(resource -> resource.getClass().getSimpleName()));

        model.addAttribute("groupedResources", groupedResources);
        model.addAttribute("username", user.getUsername().toUpperCase());
        return "viewResources";
    }

    @GetMapping("/resources/delete/{id}")
    public String deleteResource(@PathVariable int id) {
        String resourceServiceUrl = RESOURCE_SERVICE_URL + "/resources/" + id;
        restTemplate.delete(resourceServiceUrl);
        return "redirect:/resources";
    }

    @GetMapping("/resources/edit/{id}")
    public String editResourceForm(@PathVariable int id, Model model) {
        String resourceServiceUrl = RESOURCE_SERVICE_URL + "/resources/" + id;
        ResponseEntity<Resource> response = restTemplate.getForEntity(resourceServiceUrl, Resource.class);
        Resource resource = response.getBody();

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
        String resourceServiceUrl = RESOURCE_SERVICE_URL + "/resources/" + id;
        ResponseEntity<Resource> response = restTemplate.getForEntity(resourceServiceUrl, Resource.class);
        Resource resource = response.getBody();

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
            gameAccount.setGamePlatform(gamePlatform);
        } else if (resource instanceof Subscription subscription) {
            subscription.setSubscriptionType(subscriptionType);
        }

        // Update resource in ResourceService
        restTemplate.put(resourceServiceUrl, resource);

        model.addAttribute("success", "Resource updated successfully!");
        return "redirect:/resources";
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
                resource = new GameAccount(name, username, password, gamePlatform);
            } else if ("Subscription".equals(resourceType)) {
                resource = new Subscription(name, username, password, subscriptionType);
            } else {
                model.addAttribute("error", "Invalid resource type selected.");
                return "addResource";
            }

            String resourceServiceUrl = RESOURCE_SERVICE_URL + "/resources/user/" + user.getId();
            restTemplate.postForEntity(resourceServiceUrl, resource, Void.class);
            return "redirect:/resources";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while adding the resource: " + e.getMessage());
            return "addResource";
        }
    }
}
