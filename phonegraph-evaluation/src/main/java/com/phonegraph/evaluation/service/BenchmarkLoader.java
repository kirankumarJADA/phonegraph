package com.phonegraph.evaluation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonegraph.evaluation.model.BenchmarkFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * BenchmarkLoader — reads benchmark_250_questions.json (produced in
 * Step 6) from disk. The path is configurable via application.properties
 * so the file can live outside the compiled JAR.
 */
@Component
public class BenchmarkLoader {

    private final String benchmarkPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BenchmarkLoader(@Value("${phonegraph.benchmark.path}") String benchmarkPath) {
        this.benchmarkPath = benchmarkPath;
    }

    public BenchmarkFile load() throws IOException {
        File file = new File(benchmarkPath);
        if (!file.exists()) {
            throw new IOException("Benchmark file not found at: " + benchmarkPath +
                    " — copy benchmark_250_questions.json here or update phonegraph.benchmark.path");
        }
        return objectMapper.readValue(file, BenchmarkFile.class);
    }
}
