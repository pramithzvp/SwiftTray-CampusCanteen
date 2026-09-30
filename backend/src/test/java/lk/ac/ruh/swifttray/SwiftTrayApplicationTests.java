package lk.ac.ruh.swifttray;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SwiftTrayApplicationTests {
    @LocalServerPort int port;
    class Client {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrf;
        HttpResponse<String> send(String method,String path,String json) throws Exception {
            if(!method.equals("GET") && csrf==null) csrf=value(send("GET","/auth/csrf",null).body(),"token");
            var r=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api"+path)).header("Content-Type","application/json");
            if(!method.equals("GET")) r.header("X-XSRF-TOKEN",csrf);
            r.method(method,json==null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
            return http.send(r.build(),HttpResponse.BodyHandlers.ofString());
        }
        void login(String email,String password) throws Exception {
            var r=send("POST","/auth/login","{\"email\":\""+email+"\",\"password\":\""+password+"\"}");
            assertEquals(200,r.statusCode(),r.body());
        }
    }
    static String value(String json,String key) {
        var m=Pattern.compile("\""+key+"\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertTrue(m.find(),json);return m.group(1);
    }
    static long id(String json) {
        var m=Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
        assertTrue(m.find(),json);return Long.parseLong(m.group(1));
    }
    static void status(int expected,HttpResponse<String> r) { assertEquals(expected,r.statusCode(),r.body()); }
    static String order(long id,int quantity) { return "{\"items\":[{\"menuItemId\":"+id+",\"quantity\":"+quantity+"}]}"; }
    static String menu(String name,String price,boolean available) { return "{\"name\":\""+name+"\",\"description\":\"Freshly prepared meal\",\"category\":\"Meals\",\"price\":"+price+",\"available\":"+available+"}"; }
    Client register(String email) throws Exception {
        Client c=new Client();var r=c.send("POST","/auth/register","{\"name\":\"Student\",\"email\":\""+email+"\",\"password\":\"StudentTestPass123!\",\"role\":\"ADMIN\"}");
        status(201,r);assertTrue(r.body().contains("STUDENT"));assertFalse(r.body().contains("password"));c.login(email,"StudentTestPass123!");return c;
    }
    @Test void fullWorkflowAndAccessBoundaries() throws Exception {
        Client anonymous=new Client();status(200,anonymous.send("GET","/health",null));status(401,anonymous.send("GET","/orders/mine",null));
        Client admin=new Client();admin.login("admin@swifttray.test","AdminTestPass123!");
        var food=admin.send("POST","/menu-items",menu("Rice and curry","350.00",true));status(201,food);long foodId=id(food.body());
        Client student=register("student@swifttray.test");
        status(403,student.send("POST","/menu-items",menu("Unauthorised food","100",true)));
        status(403,student.send("GET","/staff/orders",null));
        status(400,student.send("POST","/orders",order(foodId,0)));
        status(400,student.send("POST","/orders","{\"items\":[]}"));
        var placed=student.send("POST","/orders",order(foodId,2));status(201,placed);long orderId=id(placed.body());
        assertTrue(placed.body().contains("700.00"),placed.body());assertTrue(placed.body().contains("QUEUED"));assertFalse(placed.body().contains("password"));
        Client another=register("another@swifttray.test");
        assertEquals("[]",another.send("GET","/orders/mine",null).body());status(404,another.send("POST","/orders/"+orderId+"/cancel",null));
        status(200,admin.send("PUT","/menu-items/"+foodId,menu("Rice and curry","400.00",true)));
        assertTrue(student.send("GET","/orders/mine",null).body().contains("700.00"));
        var tampered=student.send("POST","/orders","{\"items\":[{\"menuItemId\":"+foodId+",\"quantity\":1}],\"total\":1}");
        status(201,tampered);assertTrue(tampered.body().contains("400.00"));status(200,student.send("POST","/orders/"+id(tampered.body())+"/cancel",null));
        status(409,admin.send("PATCH","/staff/orders/"+orderId+"/status","{\"status\":\"READY\"}"));
        status(201,admin.send("POST","/admin/staff","{\"name\":\"Staff\",\"email\":\"staff@swifttray.test\",\"password\":\"StaffTestPass123!\"}"));
        Client staff=new Client();staff.login("staff@swifttray.test","StaffTestPass123!");status(403,staff.send("POST","/orders",order(foodId,1)));
        for(String next:List.of("PREPARING","READY","COLLECTED")) {
            var r=staff.send("PATCH","/staff/orders/"+orderId+"/status","{\"status\":\""+next+"\"}");status(200,r);assertTrue(r.body().contains(next));
            if(next.equals("PREPARING")) status(409,student.send("POST","/orders/"+orderId+"/cancel",null));
        }
        assertEquals("[]",staff.send("GET","/staff/orders",null).body());
        status(200,admin.send("PUT","/menu-items/"+foodId,menu("Rice and curry","400.00",false)));status(409,student.send("POST","/orders",order(foodId,1)));
        status(204,admin.send("DELETE","/menu-items/"+foodId,null));assertFalse(anonymous.send("GET","/menu-items",null).body().contains("Rice and curry"));
        assertTrue(student.send("GET","/orders/mine",null).body().contains("Rice and curry"));status(204,student.send("POST","/auth/logout",null));status(401,student.send("GET","/orders/mine",null));
    }
    @Test void concurrentOrdersReceiveDifferentTokensAndUnsafeRequestsNeedCsrf() throws Exception {
        Client admin=new Client();admin.login("admin@swifttray.test","AdminTestPass123!");
        long food=id(admin.send("POST","/menu-items",menu("Concurrency meal","200.00",true)).body());
        Client a=register("parallel-a@swifttray.test"), b=register("parallel-b@swifttray.test");ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            var first=executor.submit(()->a.send("POST","/orders",order(food,1)));var second=executor.submit(()->b.send("POST","/orders",order(food,1)));
            var r1=first.get(20,TimeUnit.SECONDS);var r2=second.get(20,TimeUnit.SECONDS);status(201,r1);status(201,r2);assertNotEquals(value(r1.body(),"token"),value(r2.body(),"token"));
            a.send("POST","/orders/"+id(r1.body())+"/cancel",null);b.send("POST","/orders/"+id(r2.body())+"/cancel",null);
        } finally { executor.shutdownNow(); }
        var noCsrf=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/auth/register"))
            .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());status(403,noCsrf);
        status(204,admin.send("DELETE","/menu-items/"+food,null));
    }
}
