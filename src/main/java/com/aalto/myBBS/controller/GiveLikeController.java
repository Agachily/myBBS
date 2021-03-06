package com.aalto.myBBS.controller;

import com.aalto.myBBS.Event.EventProducer;
import com.aalto.myBBS.service.GiveLikeService;
import com.aalto.myBBS.service.entity.Event;
import com.aalto.myBBS.service.entity.User;
import com.aalto.myBBS.util.HostHolder;
import com.aalto.myBBS.util.MybbsConstant;
import com.aalto.myBBS.util.MybbsUtil;
import com.aalto.myBBS.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

@Controller
public class GiveLikeController implements MybbsConstant {
    @Autowired
    private GiveLikeService giveLikeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String giveOrCancelLike(int entityType, int entityId, int entityUserId, int postId) {
        // Get the current user
        User user = hostHolder.getUser();
        giveLikeService.giveOrCancelLike(user.getId(), entityType, entityId, entityUserId);

        long likeNumberOfEntity = giveLikeService.findLikeNumberOfEntity(entityType, entityId);
        int status = giveLikeService.checkLikeStatusOfEntity(user.getId(), entityType, entityId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("likeCount", likeNumberOfEntity);
        map.put("likeStatus", status);

        /* Fire the given like event when the user is giving like rather than canceling like */
        if (status == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        /* ??????????????????????????????????????????????????????????????? */
        if(entityType == ENTITY_TYPE_POST) {
            // ??????????????????
            String redisKey = RedisUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        return MybbsUtil.getJSONString(200, null, map);
    }
}
