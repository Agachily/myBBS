package com.aalto.myBBS.controller;

import com.aalto.myBBS.Event.EventProducer;
import com.aalto.myBBS.service.CommentService;
import com.aalto.myBBS.service.DiscussPostService;
import com.aalto.myBBS.service.entity.Comment;
import com.aalto.myBBS.service.entity.DiscussPost;
import com.aalto.myBBS.service.entity.Event;
import com.aalto.myBBS.util.HostHolder;
import com.aalto.myBBS.util.MybbsConstant;
import com.aalto.myBBS.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements MybbsConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    /**
     * hostHolder变量用于获取当前用户Id
     */
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 添加Comment
     * @param id
     * @param comment 由Post中的内容获取，Spring MVC会自动根据Post中的内容构建一个comment对象
     * @return
     */
    @RequestMapping(path = "/add/{id}", method = RequestMethod.POST)
    public String addComment(@PathVariable("id") int id, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        // Trigger the comment event and send the system message to kafka
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", id);
        /* Sent the entityUserId, namely the author of the entity according to different entity type */
        if(comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost targetDiscuss = discussPostService.findDiscussPostById(comment.getEntityId());
            /* Set the author of the entity */
            event.setEntityUserId(targetDiscuss.getUserId());
        }
        if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment targetComment = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(targetComment.getUserId());
        }
        /* Send the Event to Kafka */
        eventProducer.fireEvent(event);

        // 当对帖子进行评论的时候，帖子对象中存储的评论数量这一数据会发生改变，因此我们需要更新ES中存储的帖子内容
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(id);
            eventProducer.fireEvent(event);

            String redisKey = RedisUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, id);
        }

        // 依旧跳转到当前帖子页面
        return "redirect:/discuss/detail/" + id;
    }
}
