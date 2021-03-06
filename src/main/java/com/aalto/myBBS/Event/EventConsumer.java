package com.aalto.myBBS.Event;

import com.aalto.myBBS.service.DiscussPostService;
import com.aalto.myBBS.service.ElasticsearchService;
import com.aalto.myBBS.service.MessageService;
import com.aalto.myBBS.service.entity.DiscussPost;
import com.aalto.myBBS.service.entity.Event;
import com.aalto.myBBS.service.entity.Message;
import com.aalto.myBBS.util.MybbsConstant;
import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EventConsumer implements MybbsConstant {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_FOLLOW, TOPIC_LIKE})
    public void handleCommentMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("The obtained message is empty");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("The message format is not correct");
            return;
        }

        /* The system should send message to the user in the background */
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        /* Set the content of the message */
        HashMap<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        // ???Event??????Map??????????????????????????????Content???
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }

        /* Convert the content object to JSON string and store it as the content of the message */
        message.setContent(JSONObject.toJSONString(content));
        /* Add the message to database */
        messageService.addMessage(message);
    }

    /**
     * ??????????????????
     */
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("The obtained message is empty");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("The message format is not correct");
            return;
        }

        /* ???MySQL???????????????????????????????????????ES??? */
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }

    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("The obtained message is empty");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("The message format is not correct");
            return;
        }

        /* ???ES??????????????? */
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }
}
