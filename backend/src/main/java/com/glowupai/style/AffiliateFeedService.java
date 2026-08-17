package com.glowupai.style;

import com.glowupai.style.StyleModels.Occasion;
import com.glowupai.style.StyleModels.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Affiliate 商品 feed 服务。
 */
@Service
public class AffiliateFeedService {

    /**
     * 日志对象。
     */
    private static final Logger log = LoggerFactory.getLogger(AffiliateFeedService.class);

    /**
     * feed 文件路径。
     */
    private final String feedPath;

    /**
     * 创建 Affiliate feed 服务。
     *
     * @param feedPath feed 文件路径
     */
    public AffiliateFeedService(@Value("${glowup.shopping.feed.path:}") String feedPath) {
        this.feedPath = feedPath;
    }

    /**
     * 按场景读取商品推荐。
     *
     * @param occasion 场景
     * @return 商品推荐列表
     */
    public List<ProductResponse> productsFor(Occasion occasion) {
        if (feedPath == null || feedPath.isBlank()) {
            return List.of();
        }
        Path path = Path.of(feedPath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            return readProducts(path, occasion);
        } catch (IOException exception) {
            log.error("affiliate feed read failed path={}", path);
            return List.of();
        }
    }

    /**
     * 从 feed 文件读取商品。
     *
     * @param path feed 文件路径
     * @param occasion 场景
     * @return 商品推荐列表
     * @throws IOException 文件读取异常
     */
    private List<ProductResponse> readProducts(Path path, Occasion occasion) throws IOException {
        List<String> lines = Files.readAllLines(path);
        if (lines.size() < 2) {
            return List.of();
        }
        char delimiter = delimiterFor(lines.getFirst());
        List<String> headers = parseDelimitedLine(lines.getFirst(), delimiter).stream()
                .map(header -> header.trim().toLowerCase(Locale.US))
                .toList();
        List<ProductResponse> products = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line == null || line.isBlank()) {
                continue;
            }
            Map<String, String> row = rowMap(headers, parseDelimitedLine(line, delimiter));
            if (!matchesOccasion(row.get("occasion"), occasion)) {
                continue;
            }
            ProductResponse product = toProduct(row);
            if (product != null) {
                products.add(product);
            }
            if (products.size() == 3) {
                break;
            }
        }
        return products;
    }

    /**
     * 解析分隔符。
     *
     * @param headerLine 表头行
     * @return 分隔符
     */
    private char delimiterFor(String headerLine) {
        return headerLine != null && headerLine.contains("\t") ? '\t' : ',';
    }

    /**
     * 解析 CSV/TSV 行。
     *
     * @param line 原始行
     * @param delimiter 分隔符
     * @return 字段列表
     */
    private List<String> parseDelimitedLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == delimiter && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    /**
     * 将表头和值合并为行 Map。
     *
     * @param headers 表头
     * @param values 字段值
     * @return 行 Map
     */
    private Map<String, String> rowMap(List<String> headers, List<String> values) {
        Map<String, String> row = new HashMap<>();
        for (int index = 0; index < headers.size() && index < values.size(); index++) {
            row.put(headers.get(index), values.get(index));
        }
        return row;
    }

    /**
     * 判断商品是否匹配场景。
     *
     * @param value feed 场景值
     * @param occasion 场景枚举
     * @return 是否匹配
     */
    private boolean matchesOccasion(String value, Occasion occasion) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        return "*".equals(normalized)
                || "all".equals(normalized)
                || occasion.label().equalsIgnoreCase(value.trim());
    }

    /**
     * 转换 feed 行为商品响应。
     *
     * @param row feed 行
     * @return 商品响应
     */
    private ProductResponse toProduct(Map<String, String> row) {
        String brand = row.getOrDefault("brand", "").trim();
        String name = row.getOrDefault("name", "").trim();
        String buyUrl = row.getOrDefault("buyurl", row.getOrDefault("buy_url", "")).trim();
        if (brand.isBlank() || name.isBlank() || buyUrl.isBlank()) {
            return null;
        }
        return new ProductResponse(
                brand,
                name,
                row.getOrDefault("tag", "Feed"),
                row.getOrDefault("price", ""),
                row.getOrDefault("reason", "Recommended from the affiliate feed."),
                buyUrl,
                row.getOrDefault("image", "")
        );
    }
}
