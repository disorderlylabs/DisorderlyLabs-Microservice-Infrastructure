package com.disorderlylabs.cart.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import com.disorderlylabs.cart.repositories.Cart;

public class CartMapper implements RowMapper {

public Cart mapRow(ResultSet rs, int rowNum) throws SQLException {  
  Cart u = new Cart();  
  u.setItemID(rs.getInt("itemID"));
  u.setQuantity(rs.getInt("quantity"));
  u.setTotalPrice(rs.getDouble("total_price"));
  u.setUserID(rs.getString("userID")); 
  return u;  
 }  
}