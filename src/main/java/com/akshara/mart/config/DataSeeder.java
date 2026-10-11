package com.akshara.mart.config;

import com.akshara.mart.model.Product;
import com.akshara.mart.model.User;
import com.akshara.mart.repo.ProductRepo;
import com.akshara.mart.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    @Autowired UserRepo users;
    @Autowired ProductRepo products;

    private User user(String name, String email, String pw, String role) {
        User u = new User();
        u.name = name; u.email = email; u.passwordHash = AuthService.hash(pw); u.role = role;
        return users.save(u);
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            user("Admin", "admin@akshara.com", "admin123", "ADMIN");
            user("Buyer Demo", "buyer@akshara.com", "buyer123", "BUYER");
        }
        if (products.count() > 0) return;
        User seller = users.findByEmail("seller@akshara.com")
                .orElseGet(() -> user("Akshara Traders", "seller@akshara.com", "seller123", "SELLER"));

        List<Product> list = new ArrayList<>();
        int[] n = {0};
        add(list, seller, n, "Washing Machines", "washing machine, washer, laundry", "washing,machine", new String[]{
            "LG 7 Kg 5 Star Front Load Washing Machine|32990|38990",
            "Samsung 8 Kg Inverter Front Load Washing Machine|35990|42990",
            "Whirlpool 7.5 Kg Top Load Washing Machine|18490|22900",
            "IFB 6.5 Kg Front Load Washing Machine|24990|29990",
            "Bosch 9 Kg Fully Automatic Washing Machine|45990|54990"});
        add(list, seller, n, "Televisions", "television, tv, smart tv, led tv", "television", new String[]{
            "Sony Bravia 43 inch 4K Ultra HD Smart LED TV|42990|54900",
            "Samsung 55 inch Crystal 4K Smart TV|49990|64900",
            "LG 32 inch HD Ready Smart LED TV|15990|21990",
            "Mi 50 inch 4K Ultra HD Google TV|28999|39999",
            "OnePlus 43 inch Full HD Android Smart TV|22999|29999"});
        add(list, seller, n, "Refrigerators", "fridge, refrigerator, cooling", "refrigerator", new String[]{
            "Samsung 253 L Double Door Refrigerator|27990|34990",
            "LG 260 L Frost Free Double Door Refrigerator|29490|36990",
            "Whirlpool 190 L Single Door Refrigerator|14990|18990",
            "Haier 320 L Triple Door Refrigerator|33990|42990",
            "Godrej 236 L Inverter Double Door Refrigerator|24990|29990"});
        add(list, seller, n, "Air Fryers", "air fryer, oil free cooking, kitchen", "air,fryer", new String[]{
            "Philips 4.1 L Digital Air Fryer|7995|10995",
            "Prestige 4 L Electric Air Fryer|5499|7499",
            "Havells 5 L Digital Air Fryer|6999|9999",
            "Agaro 3.5 L Air Fryer|3999|6499",
            "Inalsa 4.2 L Air Fryer|4499|6999"});
        add(list, seller, n, "Air Conditioners", "air conditioner, ac, split ac, cooling", "air,conditioner", new String[]{
            "Daikin 1.5 Ton 5 Star Inverter Split AC|46990|62000",
            "LG 1.5 Ton 3 Star Inverter Split AC|37990|52990",
            "Voltas 1.5 Ton 3 Star Split AC|34990|48990",
            "Carrier 1 Ton 3 Star Inverter Split AC|31990|44990",
            "Hitachi 1.5 Ton 5 Star Inverter Split AC|44990|59990"});
        add(list, seller, n, "Microwave Ovens", "microwave oven, oven, convection, kitchen", "microwave", new String[]{
            "IFB 25 L Convection Microwave Oven|11990|15990",
            "Samsung 28 L Convection Microwave Oven|12490|15490",
            "LG 20 L Solo Microwave Oven|6990|8990",
            "Panasonic 27 L Convection Microwave Oven|10990|14490",
            "Whirlpool 30 L Convection Microwave Oven|13490|17990"});
        add(list, seller, n, "Mixer Grinders", "mixer grinder, mixie, grinder, kitchen", "mixer,grinder", new String[]{
            "Preethi Zodiac 750 W Mixer Grinder|8999|12999",
            "Bajaj Rex 500 W Mixer Grinder|3299|4299",
            "Philips HL7756 750 W Mixer Grinder|4499|5995",
            "Butterfly Jet Elite 750 W Mixer Grinder|3999|5399",
            "Sujata Dynamix 900 W Mixer Grinder|7999|10499"});
        add(list, seller, n, "Vacuum Cleaners", "vacuum cleaner, cleaning, home care", "vacuum,cleaner", new String[]{
            "Eureka Forbes 1400 W Vacuum Cleaner|5999|8999",
            "Dyson V8 Absolute Cordless Vacuum Cleaner|34900|42900",
            "Philips PowerPro 1900 W Vacuum Cleaner|7999|11999",
            "Karcher WD3 Wet and Dry Vacuum Cleaner|9999|13999",
            "Mi Robot Vacuum Mop Cleaner|19999|29999"});
        add(list, seller, n, "Ceiling Fans", "ceiling fan, fan, cooling", "ceiling,fan", new String[]{
            "Havells Stealth Air 1200 mm Ceiling Fan|3999|5200",
            "Orient Aeroslim 1200 mm Ceiling Fan|2999|4100",
            "Usha Bloom 1200 mm Ceiling Fan|2799|3900",
            "Crompton Energion 1200 mm Ceiling Fan|3299|4400",
            "Atomberg Renesa 1200 mm BLDC Ceiling Fan|3399|4600"});
        add(list, seller, n, "Water Heaters", "water heater, geyser, bathroom", "water,heater", new String[]{
            "AO Smith 15 L Storage Water Heater|8999|12500",
            "Bajaj 25 L Storage Water Heater|6999|9999",
            "Racold 15 L Storage Water Heater|7499|10500",
            "Havells 25 L Storage Water Heater|7999|11000",
            "Crompton 10 L Instant Water Heater|5499|7999"});
        products.saveAll(list);
        System.out.println(">>> Akshara Mart: " + list.size() + " products added");
    }

    private void add(List<Product> list, User seller, int[] n, String cat, String tags, String kw, String[] rows) {
        for (String r : rows) {
            String[] p = r.split("\\|");
            n[0]++;
            Product x = new Product();
            x.name = p[0]; x.category = cat; x.price = Double.parseDouble(p[1]); x.mrp = Double.parseDouble(p[2]);
            x.stock = 20 + (n[0] % 7) * 5;
            x.description = p[0] + ". " + tags + ". Genuine product with 1 year manufacturer warranty and free delivery.";
            x.imageUrl = "https://loremflickr.com/500/400/" + kw + "?lock=" + n[0];
            x.sellerId = seller.id; x.sellerName = seller.name;
            list.add(x);
        }
    }
}
