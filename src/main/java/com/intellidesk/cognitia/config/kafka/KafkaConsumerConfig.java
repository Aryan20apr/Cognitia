package com.intellidesk.cognitia.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.ingestion.models.entities.IngestionOutbox;
import com.intellidesk.cognitia.ingestion.models.entities.RawSouce;



@Configuration
public class KafkaConsumerConfig {
    
    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Value("${ingestion.group.name}")
    private String ingestionGroupId;

    @Value("${analytics.usage-events.group.name}")
    private String analyticsUsageEventsGroupId;

    @Bean
    public ConsumerFactory<String, IngestionOutbox> resourceConsumerFactory(String groupId){
       Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName()); // Add this line
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.intellidesk.cognitia.ingestion.models.entity");
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, RawSouce.class.getName());
    return new DefaultKafkaConsumerFactory<>(props);
}

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IngestionOutbox> ingestionKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String,IngestionOutbox> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(resourceConsumerFactory(ingestionGroupId));
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ChatUsageDetailsDTO> usageEventsConsumerFactory(){
       Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, analyticsUsageEventsGroupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName()); // Add this line
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.intellidesk.cognitia.analytics.models.dto");
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ChatUsageDetailsDTO.class.getName());
    return new DefaultKafkaConsumerFactory<>(props);
}


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,ChatUsageDetailsDTO> usageEventsKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String,ChatUsageDetailsDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(usageEventsConsumerFactory());
        return factory;
    }
}
