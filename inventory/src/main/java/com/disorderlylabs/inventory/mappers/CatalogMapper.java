package com.disorderlylabs.inventory.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import com.disorderlylabs.inventory.repositories.Catalog;

public class CatalogMapper implements RowMapper {

public Catalog mapRow(ResultSet rs, int rowNum) throws SQLException {  
  Catalog u = new Catalog();  
  u.setItemID(rs.getInt("itemID"));
  u.setName(rs.getString("name"));
  u.setQuantity(rs.getInt("quantity"));
  u.setPrice(rs.getDouble("price")); 
  return u;  
 }  
}