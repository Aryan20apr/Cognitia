package com.intellidesk.cognitia.userandauth.models.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsDTO{
    
    String userId;
    String name; 
    String email; 
    String phoneNumber; 
    String role; 
    String companyId; 
    String Role; 
    List<String> permissions;  
    
}
