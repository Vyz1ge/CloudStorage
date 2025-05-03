//package com.visage.cloudstorage.Services;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.io.Decoders;
//import io.jsonwebtoken.security.Keys;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Service;
//
//import java.security.Key;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Function;
//
//@Service
//public class JwtService {
//
//    private static final String SECRET_KEY = "305c300d06092a864886f70d0101010500034b003048024100b349aebde72d42ebdb438aafc5fde19cc29706206ee944cbfe8b15afc2c802111657fbed3b305c481702b7512a1d2510ffd55edb959f609d79e94bfe89d769ad0203010001";
//
//    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
//        final Claims claims = extractAllClaims(token);
//        return claimsResolver.apply(claims);
//    }
//
//    public String extractUsername(String token) {
//        return extractClaim(token,Claims::getSubject);
//    }
//
//
//    public String generateToken(
//            Map<String, Object> extraClaims,
//            UserDetails userDetails
//    ){
//        return Jwts
//                .builder()
//                .setClaims(extraClaims)
//                .setSubject(userDetails.getUsername())
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() +1000 *60*24))
//                .signWith(getSingInKey(), SignatureAlgorithm.HS512)
//                .compact();
//    }
//
//
//    public boolean isTokenValid(String token,UserDetails userDetails){
//        final String username = extractUsername(token);
//        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
//    }
//
//    private boolean isTokenExpired(String token) {
//        return extractExporation(token).before(new Date());
//    }
//
//    private Date extractExporation(String token) {
//        return extractClaim(token, Claims::getExpiration);
//    }
//
//
//    public String generateToken(UserDetails userDetails){
//        return generateToken(new HashMap<>(),userDetails);
//    }
//
//
//    private Claims extractAllClaims(String token){
//        return Jwts
//                .parserBuilder()
//                .setSigningKey(getSingInKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//    }
//
//    private Key getSingInKey() {
//        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
//        return Keys.hmacShaKeyFor(keyBytes);
//    }
//}
