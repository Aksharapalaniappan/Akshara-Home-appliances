package com.akshara.mart.controller;

import com.akshara.mart.config.AuthService;
import com.akshara.mart.model.*;
import com.akshara.mart.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired UserRepo users;
    @Autowired ProductRepo products;
    @Autowired CartRepo carts;
    @Autowired OrderRepo orders;
    @Autowired AuthService auth;
    @Value("${app.upload-dir}") String uploadDir;

    private static final String TOKEN = "X-Auth-Token";

    private ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }

    // ---------- AUTH ----------
    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Map<String, String> b) {
        String name = b.getOrDefault("name", "").trim();
        String email = b.getOrDefault("email", "").trim().toLowerCase();
        String pw = b.getOrDefault("password", "");
        String role = "SELLER".equals(b.get("role")) ? "SELLER" : "BUYER";
        if (name.isEmpty() || email.isEmpty() || pw.length() < 4) throw bad("Fill all fields. Password needs at least 4 characters");
        if (users.findByEmail(email).isPresent()) throw bad("This email is already registered");
        User u = new User();
        u.name = name; u.email = email; u.passwordHash = AuthService.hash(pw); u.role = role;
        users.save(u);
        return auth.session(u);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> b) {
        String email = b.getOrDefault("email", "").trim().toLowerCase();
        User u = users.findByEmail(email).orElse(null);
        if (u == null || !u.passwordHash.equals(AuthService.hash(b.getOrDefault("password", ""))))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        String want = b.get("role");
        if (want != null && !want.equals(u.role))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This is not a " + want.toLowerCase() + " account");
        return auth.session(u);
    }

    // ---------- PRODUCTS ----------
    @GetMapping("/products")
    public List<Product> list(@RequestParam(required = false) String search, @RequestParam(required = false) String category) {
        List<Product> l = (search != null && !search.isBlank()) ? products.search(search.trim()) : products.findAll();
        if (category != null && !category.isBlank() && !category.equals("All"))
            l = l.stream().filter(p -> category.equals(p.category)).collect(Collectors.toList());
        return l;
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return products.findAll().stream().map(p -> p.category).distinct().sorted().collect(Collectors.toList());
    }

    private void fill(Product p, Map<String, Object> b) {
        String name = String.valueOf(b.getOrDefault("name", "")).trim();
        String cat = String.valueOf(b.getOrDefault("category", "")).trim();
        double price, mrp; int stock;
        try {
            price = Double.parseDouble(String.valueOf(b.get("price")));
            mrp = b.get("mrp") == null || String.valueOf(b.get("mrp")).isBlank() ? price : Double.parseDouble(String.valueOf(b.get("mrp")));
            stock = Integer.parseInt(String.valueOf(b.getOrDefault("stock", "1")));
        } catch (Exception e) { throw bad("Price, MRP and stock must be numbers"); }
        if (name.isEmpty() || cat.isEmpty() || price <= 0 || stock < 0) throw bad("Name, category, price and stock are required");
        p.name = name; p.category = cat; p.price = price; p.mrp = Math.max(mrp, price); p.stock = stock;
        p.description = String.valueOf(b.getOrDefault("description", ""));
        String img = String.valueOf(b.getOrDefault("imageUrl", "")).trim();
        p.imageUrl = img.isEmpty() || img.equals("null") ? p.imageUrl : img;
    }

    @PostMapping("/products")
    public Product addProduct(@RequestHeader(value = TOKEN, required = false) String t, @RequestBody Map<String, Object> b) {
        User u = auth.require(t, "SELLER", "ADMIN");
        Product p = new Product();
        fill(p, b);
        p.sellerId = u.id; p.sellerName = u.name;
        return products.save(p);
    }

    @PutMapping("/products/{id}")
    public Product updateProduct(@RequestHeader(value = TOKEN, required = false) String t, @PathVariable Long id, @RequestBody Map<String, Object> b) {
        User u = auth.require(t, "SELLER", "ADMIN");
        Product p = products.findById(id).orElseThrow(() -> bad("Product not found"));
        if (!"ADMIN".equals(u.role) && !u.id.equals(p.sellerId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your product");
        fill(p, b);
        return products.save(p);
    }

    @DeleteMapping("/products/{id}")
    public Map<String, Object> deleteProduct(@RequestHeader(value = TOKEN, required = false) String t, @PathVariable Long id) {
        User u = auth.require(t, "SELLER", "ADMIN");
        Product p = products.findById(id).orElseThrow(() -> bad("Product not found"));
        if (!"ADMIN".equals(u.role) && !u.id.equals(p.sellerId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your product");
        products.delete(p);
        return Map.of("deleted", true);
    }

    @GetMapping("/seller/products")
    public List<Product> myProducts(@RequestHeader(value = TOKEN, required = false) String t) {
        User u = auth.require(t, "SELLER", "ADMIN");
        return products.findBySellerId(u.id);
    }

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestHeader(value = TOKEN, required = false) String t, @RequestParam("file") MultipartFile f) throws IOException {
        auth.require(t, "SELLER", "ADMIN");
        String orig = f.getOriginalFilename() == null ? "img.jpg" : f.getOriginalFilename().toLowerCase();
        if (!orig.matches(".*\\.(jpg|jpeg|png|webp|gif)$")) throw bad("Only jpg, png, webp or gif images allowed");
        String ext = orig.substring(orig.lastIndexOf('.'));
        Path dir = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(dir);
        String fname = UUID.randomUUID() + ext;
        Files.copy(f.getInputStream(), dir.resolve(fname));
        return Map.of("url", "/uploads/" + fname);
    }

    // ---------- CART ----------
    @GetMapping("/cart")
    public List<Map<String, Object>> cart(@RequestHeader(value = TOKEN, required = false) String t) {
        User u = auth.require(t, "BUYER");
        List<Map<String, Object>> out = new ArrayList<>();
        for (CartItem c : carts.findByUserId(u.id)) {
            Product p = products.findById(c.productId).orElse(null);
            if (p == null) { carts.delete(c); continue; }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.id); m.put("productId", p.id); m.put("name", p.name); m.put("imageUrl", p.imageUrl);
            m.put("price", p.price); m.put("stock", p.stock); m.put("quantity", c.quantity);
            out.add(m);
        }
        return out;
    }

    @PostMapping("/cart")
    public Map<String, Object> addToCart(@RequestHeader(value = TOKEN, required = false) String t, @RequestBody Map<String, Object> b) {
        User u = auth.require(t, "BUYER");
        Long pid = Long.valueOf(String.valueOf(b.get("productId")));
        int qty = Integer.parseInt(String.valueOf(b.getOrDefault("quantity", 1)));
        Product p = products.findById(pid).orElseThrow(() -> bad("Product not found"));
        if (p.stock <= 0) throw bad("Out of stock");
        CartItem c = carts.findByUserIdAndProductId(u.id, pid).orElseGet(() -> {
            CartItem x = new CartItem(); x.userId = u.id; x.productId = pid; x.quantity = 0; return x; });
        c.quantity = Math.min(c.quantity + qty, p.stock);
        carts.save(c);
        return Map.of("count", carts.findByUserId(u.id).stream().mapToInt(i -> i.quantity).sum());
    }

    @PutMapping("/cart/{id}")
    public Map<String, Object> updateCart(@RequestHeader(value = TOKEN, required = false) String t, @PathVariable Long id, @RequestBody Map<String, Object> b) {
        User u = auth.require(t, "BUYER");
        CartItem c = carts.findById(id).orElseThrow(() -> bad("Item not found"));
        if (!c.userId.equals(u.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your cart");
        int qty = Integer.parseInt(String.valueOf(b.get("quantity")));
        Product p = products.findById(c.productId).orElseThrow(() -> bad("Product not found"));
        if (qty <= 0) carts.delete(c); else { c.quantity = Math.min(qty, p.stock); carts.save(c); }
        return Map.of("ok", true);
    }

    @DeleteMapping("/cart/{id}")
    public Map<String, Object> removeCart(@RequestHeader(value = TOKEN, required = false) String t, @PathVariable Long id) {
        User u = auth.require(t, "BUYER");
        carts.findById(id).ifPresent(c -> { if (c.userId.equals(u.id)) carts.delete(c); });
        return Map.of("ok", true);
    }

    // ---------- ORDERS ----------
    @PostMapping("/orders")
    @Transactional
    public Orders placeOrder(@RequestHeader(value = TOKEN, required = false) String t, @RequestBody Map<String, String> b) {
        User u = auth.require(t, "BUYER");
        String address = b.getOrDefault("address", "").trim();
        if (address.length() < 5) throw bad("Please enter a delivery address");
        List<CartItem> items = carts.findByUserId(u.id);
        if (items.isEmpty()) throw bad("Your cart is empty");
        Orders o = new Orders();
        o.userId = u.id; o.userName = u.name; o.address = address;
        o.paymentMethod = b.getOrDefault("paymentMethod", "Cash on Delivery");
        o.status = "PLACED"; o.createdAt = LocalDateTime.now();
        double total = 0;
        for (CartItem c : items) {
            Product p = products.findById(c.productId).orElseThrow(() -> bad("A product in your cart is no longer available"));
            if (p.stock < c.quantity) throw bad(p.name + " has only " + p.stock + " left in stock");
            p.stock -= c.quantity;
            products.save(p);
            OrderItem oi = new OrderItem();
            oi.productId = p.id; oi.productName = p.name; oi.imageUrl = p.imageUrl;
            oi.price = p.price; oi.quantity = c.quantity; oi.sellerId = p.sellerId;
            o.items.add(oi);
            total += p.price * c.quantity;
        }
        o.total = total;
        orders.save(o);
        carts.deleteAll(items);
        return o;
    }

    @GetMapping("/orders")
    public List<Orders> myOrders(@RequestHeader(value = TOKEN, required = false) String t) {
        User u = auth.require(t, "BUYER");
        return orders.findByUserIdOrderByCreatedAtDesc(u.id);
    }

    @GetMapping("/seller/orders")
    public List<Map<String, Object>> sellerOrders(@RequestHeader(value = TOKEN, required = false) String t) {
        User u = auth.require(t, "SELLER");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Orders o : orders.findAllByOrderByCreatedAtDesc()) {
            List<OrderItem> mine = o.items.stream().filter(i -> u.id.equals(i.sellerId)).collect(Collectors.toList());
            if (mine.isEmpty()) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.id); m.put("customer", o.userName); m.put("address", o.address); m.put("status", o.status);
            m.put("createdAt", o.createdAt); m.put("items", mine);
            out.add(m);
        }
        return out;
    }

    @PutMapping("/orders/{id}/status")
    public Map<String, Object> setStatus(@RequestHeader(value = TOKEN, required = false) String t, @PathVariable Long id, @RequestBody Map<String, String> b) {
        auth.require(t, "SELLER", "ADMIN");
        String s = b.getOrDefault("status", "");
        if (!List.of("PLACED", "SHIPPED", "DELIVERED").contains(s)) throw bad("Invalid status");
        Orders o = orders.findById(id).orElseThrow(() -> bad("Order not found"));
        o.status = s; orders.save(o);
        return Map.of("ok", true);
    }

    // ---------- ADMIN ----------
    @GetMapping("/admin/users")
    public List<User> allUsers(@RequestHeader(value = TOKEN, required = false) String t) {
        auth.require(t, "ADMIN");
        return users.findAll();
    }

    @DeleteMapping("/admin/users/{id}")
    public Map<String, Object> deleteUser(@RequestHeader(value = TOKEN, required = false) String t, @PathVariable Long id) {
        User me = auth.require(t, "ADMIN");
        if (me.id.equals(id)) throw bad("You cannot delete yourself");
        users.deleteById(id);
        return Map.of("ok", true);
    }

    @GetMapping("/admin/orders")
    public List<Orders> allOrders(@RequestHeader(value = TOKEN, required = false) String t) {
        auth.require(t, "ADMIN");
        return orders.findAllByOrderByCreatedAtDesc();
    }
}
