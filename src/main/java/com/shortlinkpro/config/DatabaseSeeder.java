package com.shortlinkpro.config;

import com.shortlinkpro.dto.UserRegisterDto;
import com.shortlinkpro.entity.User;
import com.shortlinkpro.entity.Url;
import com.shortlinkpro.entity.ClickAnalytics;
import com.shortlinkpro.repository.UserRepository;
import com.shortlinkpro.repository.UrlRepository;
import com.shortlinkpro.repository.ClickAnalyticsRepository;
import com.shortlinkpro.service.UserService;
import com.shortlinkpro.service.QrCodeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Random;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final UserService userService;
    private final QrCodeService qrCodeService;

    public DatabaseSeeder(UserRepository userRepository, UrlRepository urlRepository,
                          ClickAnalyticsRepository clickAnalyticsRepository, UserService userService,
                          QrCodeService qrCodeService) {
        this.userRepository = userRepository;
        this.urlRepository = urlRepository;
        this.clickAnalyticsRepository = clickAnalyticsRepository;
        this.userService = userService;
        this.qrCodeService = qrCodeService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            System.out.println("No users found. Seeding default demo data...");
            
            UserRegisterDto demoUser = new UserRegisterDto();
            demoUser.setUsername("demo");
            demoUser.setEmail("demo@shortlinkpro.com");
            demoUser.setPassword("password123");
            User savedUser = userService.registerUser(demoUser);

            String baseUrl = "http://localhost:8080";

            Url googleUrl = createUrl(savedUser, "https://www.google.com", "google", baseUrl);
            Url githubUrl = createUrl(savedUser, "https://github.com", "github", baseUrl);
            Url springUrl = createUrl(savedUser, "https://spring.io", "spring", baseUrl);
            Url notionUrl = createUrl(savedUser, "https://www.notion.so", "notion", baseUrl);
            Url expiredUrl = createUrl(savedUser, "https://expired-link.com", "expired", baseUrl);
            
            expiredUrl.setStatus("EXPIRED");
            expiredUrl.setExpiryDate(LocalDateTime.now().minusDays(1));
            urlRepository.save(expiredUrl);

            Random random = new Random();
            String[] devices = {"Desktop", "Mobile", "Tablet"};
            String[] browsers = {"Chrome", "Firefox", "Safari", "Edge", "Opera"};
            String[] countries = {"United States", "India", "United Kingdom", "Germany", "Canada", "Australia", "France", "Japan", "Brazil", "Singapore"};
            String[] referrers = {"google.com", "github.com", "t.co", "linkedin.com", "news.ycombinator.com", "Direct / Email"};

            LocalDateTime now = LocalDateTime.now();
            Url[] urls = {googleUrl, githubUrl, springUrl, notionUrl};

            for (Url url : urls) {
                int clicksCount = 15 + random.nextInt(35);
                for (int i = 0; i < clicksCount; i++) {
                    ClickAnalytics click = new ClickAnalytics();
                    click.setUrl(url);
                    click.setDevice(devices[random.nextInt(devices.length)]);
                    click.setBrowser(browsers[random.nextInt(browsers.length)]);
                    click.setCountry(countries[random.nextInt(countries.length)]);
                    click.setReferrer(referrers[random.nextInt(referrers.length)]);
                    click.setIpAddress("192.168.1." + random.nextInt(254));
                    click.setClickTime(now.minusDays(random.nextInt(20)).minusHours(random.nextInt(24)));
                    clickAnalyticsRepository.save(click);

                    url.incrementClicks();
                }
                urlRepository.save(url);
            }

            System.out.println("Database seeding completed! Demo User: demo / password123");
        }
    }

    private Url createUrl(User user, String originalUrl, String code, String baseUrl) {
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setShortCode(code);
        url.setCustomAlias(code);
        url.setUser(user);
        url.setStatus("ACTIVE");
        
        String qrBase64 = qrCodeService.generateQrCodeBase64(baseUrl + "/r/" + code, 300, 300);
        url.setQrCode(qrBase64);

        return urlRepository.save(url);
    }
}
