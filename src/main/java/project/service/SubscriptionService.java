package project.service;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import project.dao.ProductDao;
import project.dao.TokenDao;
import project.dao.UserDao;
import project.domain.Product;
import project.domain.Token;
import project.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Log4j
@EnableScheduling
@Service
public class SubscriptionService {

    @Value("${bot_info.adminId}")
    private Long adminId;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ScrapingService scrapingService;

    @Lazy
    @Autowired
    private TelegramBotService telegramBotService;

    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();

            //commands
            switch (text) {
                case "/start" -> start(update);
                case "الاشتراک" -> registerSub(update);
                case "تاريخ تسجيلي" -> sendUserRegistrationDate(update);
                case "التحقق من توفر المنتجات" -> newsAllProducts(update);
                case "/newtoken" -> adminGenerateNewToken(update);
                case "/allsubscribers" -> allSubscribers(update);
                default -> {
                    if (text.startsWith("tk-")) {
                        getToken(update);
                    }
                }
            }
        }
    }

    //start
    public void start(Update update) {

        if (!isRegistered(update.getMessage().getFrom().getId())) {

            User user = new User();
            user.setId(new Random().nextLong());
            user.setUserId(update.getMessage().getFrom().getId());
            user.setRegistrationDate(LocalDateTime.now());
            userDao.save(user);

            if (isAdmin(update)) {
                telegramBotService.sendTextMessage("أنت ادمین ولا تحتاج للاشتراک", adminId);
            }

            if (!isAdmin(update)) {
                telegramBotService.sendTextMessage("للاشتراك وتلقي التنبيهات، يرجى أرسل رسالة إلى هذا الحساب\n @admin"
                        ,update.getMessage().getFrom().getId());

            }
        }
    }

    //الاشتراک
    public void registerSub(Update update) {

        long userId = update.getMessage().getFrom().getId();

        if (isAdmin(update)) {
            telegramBotService.sendTextMessage("أنت ادمین ولا تحتاج للاشتراک", userId);
        } else  {
            telegramBotService.sendTextMessage("للاشتراك وتلقي التنبيهات، يرجى أرسل رسالة إلى هذا الحساب\n @admin", userId);
        }
    }

    //تاريخ تسجيلي
    public void sendUserRegistrationDate(Update update) {

        LocalDateTime date = getUserRegistrationDate(update);
        if (date != null) {
            telegramBotService.sendTextMessage("لقد قمت بالتسجيل في الروبوت في:\n "
                            .concat(date.toString().substring(0, 19))
                    , update.getMessage().getFrom().getId());
        } else if (date == null) {
            telegramBotService.sendTextMessage("خطأ، الرجاء إعادة تشغيل البوت"
                    , update.getMessage().getFrom().getId());
        }
    }

    //التحقق من توفر المنتجات
    public void newsAllProducts(Update update) {

        if ((!isSubscriber(update.getMessage().getFrom().getId()))) {
            if (!isAdmin(update)) {
                telegramBotService.sendTextMessage("يرجى التسجيل أولاً", update.getMessage().getFrom().getId());
                telegramBotService.sendTextMessage("للاشتراك وتلقي التنبيهات، يرجى أرسل رسالة إلى هذا الحساب\n @admin"
                        ,update.getMessage().getFrom().getId());
            }
        }

        //todo remove hardcode
        if ((isAdmin(update)) || (isSubscriber(update.getMessage().getFrom().getId())) || update.getMessage().getFrom().getId().equals(749852523) ) {

            List<Product> productList = productDao.findAll();

            if (productList != null){

                for (Product product : productList) {

                    String productMessage = "";


                    productMessage = productMessage.concat(String
                            .format("المنتج: %s \n" +
                                            "توفر: %s \n" +
                                            "رابط:\n %s \n"
                                    , product.getName(), ((product.getAvailability() !=null) && product.getAvailability())?
                                            "متوفر الان✅":"غير متوفر ❌", product.getLink()));

                    telegramBotService.sendTextMessage(productMessage, update.getMessage().getFrom().getId());
                }

            }

        }

    }

    //newtoken
    public void adminGenerateNewToken(Update update) {

        if (isAdmin(update)) {
            Token token = new Token();
            String tokenCode = "tk-".concat(generateShortUUID());
            token.setId(new Random().nextLong());
            token.setTokenCode(tokenCode);
            token.setGenerationDate(LocalDate.now());
            token.setExpirationDate(LocalDate.now().plus(Period.ofMonths(1)));
            token.setRelatedUserId(null);
            tokenDao.save(token);
            telegramBotService.sendTextMessage(tokenCode, adminId);
        }
    }

    //allSubscribers
    public void allSubscribers(Update update) {

        if (isAdmin(update)) {

            Integer allSubscribers = tokenDao.allSubscribers();

            String log = String.format("نوجد: %s مشتركين", allSubscribers.toString());

            telegramBotService.sendTextMessage(log, adminId);
        }

    }

    //tk-
    public void getToken(Update update) {

        long userId = update.getMessage().getFrom().getId();

        if (isValidToken(update.getMessage().getText())) {

            Token token = tokenDao.findTokenByTokenCode(update.getMessage().getText());

            if (token.getRelatedUserId() != null) {

                if (token.getRelatedUserId() == userId) {

                    telegramBotService.sendTextMessage("هذا الرمز هو لك", userId);
                }
            } else {
                if ((token.getRelatedUserId() == null) && (!isAdmin(update))) {
                    if (!isSubscriber(userId)) {
                        token.setRelatedUserId(userId);
                    }
                }
            }
            tokenDao.updateToken(token);

            if (isAdmin(update) || userId == token.getRelatedUserId()) {
                telegramBotService.sendTextMessage("تاريخ انتهاء الاشتراك: \n"
                        .concat(token.getExpirationDate().toString()), userId);
            }

        } else if (!isValidToken(update.getMessage().getText())) {
            telegramBotService.sendTextMessage("لم يتم العثور على معرفك في قاعدة البيانات. يرجى التسجيل أولاً.", userId);
            telegramBotService.sendTextMessage("للاشتراك وتلقي التنبيهات، يرجى أرسل رسالة إلى هذا الحساب\n @admin", userId);
        }

    }

    @Scheduled(fixedRate = 60000)
    public void notifySubscribers() {

        List<Product> newProducts = scrapingService.getAllProducts();
        List<Product> oldProducts = productDao.findAll();
        Map<String, Boolean> oldProductsMap = new HashMap<>();
        List<Product> changedAvailability = new ArrayList<>();

        if (!oldProducts.isEmpty()) {

            oldProducts.forEach(product -> oldProductsMap.put(product.getName(), product.getAvailability()));

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

            if (!changedAvailability.isEmpty()){

                List<Long> userIdList = tokenDao.allUserIdList()
                        .stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                //todo remove hardcode
            if (!userIdList.contains(adminId)) userIdList.add(adminId);
//            long rayan =749852523;
//            if (!userIdList.contains(rayan)) userIdList.add(rayan);

                for (Long userId : userIdList) {
                    if (isSubscriber(userId) || userId.equals(adminId)) {

                        for (Product product : changedAvailability) {
                            telegramBotService.sendTextMessage(String
                                    .format("منتج: %s ✅ متاح الآن ✅\nالرابط: %s "
                                            , product.getName(), product.getLink()), userId);
                        }
                    }
                }

            }

            if (newProducts != null) newProducts.forEach(productDao::updateProductStatus);

        } else {
            Optional.ofNullable(newProducts).ifPresent(productDao::saveAll);
        }

    }

    @Scheduled(fixedRate = 86400000)
    public void deletePastMonthTokens() {
        LocalDate pastMonth = LocalDate.now().minusMonths(1);
        tokenDao.deleteOldTokens(pastMonth);
    }

    public static String generateShortUUID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, Math.min(8, uuid.length()));
    }

    public LocalDateTime getUserRegistrationDate(Update update) {
        return userDao.getUserRegistrationDate(update.getMessage().getFrom().getId());
    }

    public boolean isAdmin(Update update) {
        return update.getMessage().getFrom().getId().equals(adminId);
    }

    public Boolean isValidToken(String tokenCode) {
        return tokenDao.isValidToken(tokenCode) > 0;
    }

    public boolean isSubscriber(long userId) {
        return tokenDao.isSubscriber(userId) > 0;
    }

    public boolean isRegistered(long userId) {
        return userDao.isRegistered(userId) > 0;
    }

}