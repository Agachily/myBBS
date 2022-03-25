package com.aalto.myBBS.controller;

import com.aalto.myBBS.entity.User;
import com.aalto.myBBS.service.UserService;
import com.aalto.myBBS.util.MybbsConstant;
import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author zetong
 * @description Deal with the login function
 */
@Controller
public class LoginController implements MybbsConstant {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    /**
     * Get the page for registration
     * @return The registration page
     */
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        // Return the register.html under the folder templates/static
        return "/site/register";
    }

    /**
     * Process the user data for registration
     * @return
     */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        // The user object received the posted data
        Map<String, Object> map = userService.register(user);

        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "An activation Email is sent, please check in the registered Email");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            // If the registration fails, return the original registration page
            // Remember the user object will be put into spring mvc automatically
            return "/site/register";
        }
    }

    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "The activation is successful");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "The activation is invalid, the account id already in use");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "The activation fails, the link is not correct");
            model.addAttribute("target", "/index");
        }

        return "/site/operate-result";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        // Return the register.html under the folder templates/static
        return "/site/login";
    }

    /**
     * This endpoint is used to return a authorisation picture for the user
     * @param response
     * @param session
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        // Generate the code
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // Store the code into session
        session.setAttribute("kaptcha", text);

        // Send the picture to the browser
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("Fail to response the code" + e.getMessage());
        }
    }
}
