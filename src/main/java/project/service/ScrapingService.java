package project.service;

import lombok.extern.log4j.Log4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import project.dao.ProductDao;
import project.dao.TokenDao;
import project.domain.Product;
import java.time.LocalDateTime;
import java.util.*;

@EnableScheduling
@Log4j
@Component
public class ScrapingService {

    @Value("${bot_info.adminId}")
    private Long adminId;

    @Lazy
    @Autowired
    private SubscriptionService subscriptionService;

    @Lazy
    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private ProductDao productDao;

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
            log.error("An unexpected error occurred: " + e.getMessage(), e);
        }
        return products;
    }

    @Scheduled(fixedRate = 60000)
    public void notifySubscribers() {

        List<Product> newProducts = getAllProducts();
        List<Product> oldProducts = productDao.findAll();

        if (!oldProducts.isEmpty()) {

            Map<String, Boolean> oldProductsMap = new HashMap<>();
            List<Product> changedAvailability = new ArrayList<>();
            List<Long> userIdList = tokenDao.allUserIdList();
            userIdList.removeIf(Objects::isNull);

            for (Product product : oldProducts) {
                oldProductsMap.put(product.getName(), product.getAvailability());
            }

            if (newProducts != null){

                for (Product product : newProducts) {

                    Boolean oldStatus = oldProductsMap.get(product.getName());
                    if (product.getAvailability() != null){
                        if (oldStatus != null && !oldStatus && product.getAvailability()) {
                            changedAvailability.add(product);
                        }
                    }
                }

            }

            //todo remove hardcode
//            if (!userIdList.contains(adminId)) userIdList.add(adminId);
//            long rayan =749852523;
//            if (!userIdList.contains(rayan)) userIdList.add(rayan);

            for (Long userId : userIdList) {
                if (subscriptionService.isSubscriber(userId) || userId.equals(adminId)) {

                    for (Product product : changedAvailability) {
                        telegramBotService.sendTextMessage(String
                                .format("منتج: %s ✅ متاح الآن ✅\nالرابط: %s "
                                        , product.getName(), product.getLink()), userId);
                    }
                }
            }

            if (newProducts != null){

                for (Product product : newProducts) {
                    productDao.updateProductStatus(product);
                }

            }

        } else {

            if (newProducts != null){
                productDao.saveAll(newProducts);
            }
        }

    }

}