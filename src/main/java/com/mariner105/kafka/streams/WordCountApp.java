package com.mariner105.kafka.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.Properties;

public class WordCountApp {

    public static final String TOPIC_WORD_COUNT_INPUT = "word-count-input";

    public static void main(String[] args) {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "word-count-application");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        // 1 - stream from Kafka
        KStream<String, String> wordCountInput = builder.stream(TOPIC_WORD_COUNT_INPUT);
        // 2 - map values to lowercase
        KTable<String, Long> wordCounts = wordCountInput
                .mapValues(textLine -> textLine.toLowerCase())

        // 3 - flatmap values split by space
                .flatMapValues(lowerCasedTextLine -> Arrays.asList(lowerCasedTextLine.split(" ")))

        // 4 - select key to apply a key (we discard the old key)
                .selectKey((ignoredKey, word) -> word)

        // 5 - group by key before aggregation
                .groupByKey()

        // 6 - count occurrences
                .count();

        // 7 - to in order to write the results back to Kafka
        wordCounts.toStream().to("word-count-output",
                (Produced<String, Long>) Produced.with(Serdes.String(), Serdes.Long()));

        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        // print the topology
        System.out.println(streams.toString());

        // shutdown hook to correctly close the streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
