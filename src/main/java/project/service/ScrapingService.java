package project.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import project.domain.Product;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class ScrapingService {

    public List<Product> getAllProducts() {

        String allProductsUrl = "https://www.dzrt.com/en/our-products.html";
        List<Product> products = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(allProductsUrl).get();
            Elements productList = doc.select("#layer-product-list > div.products.wrapper.grid.products-grid > ol > li");
            for (Element productElement : productList) {
                String productName = productElement.select("div.visible-front > strong").text();
                String productLink = productElement.select("a.product-item-link").attr("href");
                Boolean availability = !productElement.select("div.product.details.product-item-details > div > div.visible-back > div > div > div.actions-primary > div > span").text().equals("Back In Stock Soon");
                Product product = new Product(new Random().nextLong(), productName, availability, productLink, LocalDateTime.now());
                products.add(product);
            }

        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
        }
        return products;
    }

}