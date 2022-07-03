package com.example.demo.controller;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Optional;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LoginDto;
import com.example.demo.dto.SignUpDto;
import com.example.demo.model.CustomUserDetails;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.OTPService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    OTPService otpService;
   
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignUpDto signUpDto){
         
    	Calendar date = Calendar.getInstance();
    	User user = new User();
    	
        // add check for username exists in a DB
        if(userRepository.existsByUsername(signUpDto.getUsername())){
            return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        // add check for email exists in DB
        if(userRepository.existsByEmail(signUpDto.getEmail())){
            return new ResponseEntity<>("Email is already taken!", HttpStatus.BAD_REQUEST);
        }
        
        // add check for mobile number  exists in DB
        if(userRepository.existsByMobileNo(signUpDto.getMobileNo())){
            return new ResponseEntity<>("Mobile number already registered!", HttpStatus.BAD_REQUEST);
        }
        
        // create user object
        prepareUserObject(signUpDto, date, user);
        Role roles = roleRepository.findByName("ROLE_USER").get();
        user.setRoles(Collections.singleton(roles));
        userRepository.save(user);
        return new ResponseEntity<>("User registered successfully.Please verify your email within 120 second otherwise registration will be locked for next 6 month!", HttpStatus.OK);
    }

    //role creation
    @PostMapping("/role")
    public ResponseEntity<?> createRole(@RequestBody Role role){
    	roleRepository.save(role);
    	return new ResponseEntity<>("Role Created successfully", HttpStatus.OK);
    }
    
    
    //below url will be sent to users email id
    @PostMapping("/user/verify")
    public ResponseEntity<?> verifyUser(@RequestParam String user){
       	User currentUser = userRepository.findByUsername(user).get();
    	Calendar date2 = Calendar.getInstance();
    	if (!userRepository.existsByUsername(user)) {
    		return new ResponseEntity<>("You have not registered yet.", HttpStatus.NOT_FOUND);
    	} 
         
    	else if  (!userRepository.existsByEmail(currentUser.getEmail())) {
    		return new ResponseEntity<>("You have not registered yet.", HttpStatus.NOT_FOUND);
    	}
    	else if(currentUser.getVerified()) {
    		return new ResponseEntity<>("You are already verified, Kindly login!!", HttpStatus.NOT_FOUND);
    	}
    	
    	else if (date2.getTimeInMillis()-currentUser.getRegistartionDate().getTimeInMillis() >20000)
    			{
    		currentUser.setVerified(false);//user not verified within time limit so it is deactivated for 6 month
    		date2.add(Calendar.MONTH, 6);
    		currentUser.setUnlockTime(date2);
    		userRepository.save(currentUser);
    		return new ResponseEntity<>("Please verify your email within 2 minute",HttpStatus.NOT_ACCEPTABLE);
    	}
    	currentUser.setVerified(true);
    	userRepository.save(currentUser);
    	return new ResponseEntity<>("Your email address is successfully verfied!!", HttpStatus.OK);
    }   
    
     
    @PostMapping("/signin")
    public ResponseEntity<String> authenticateUser(@RequestBody LoginDto loginDto){
    	
    	Optional<User> user = userRepository.findByUsernameOrEmail(loginDto.getUsernameOrEmail(), loginDto.getUsernameOrEmail());
    	if (!user.isPresent())
    	{
    		return new ResponseEntity<>("Username Not registered!!", HttpStatus.OK);
    	}
    	if (!user.get().getVerified())
    	{
    		return new ResponseEntity<>("User not verified!!", HttpStatus.OK);
    	}
    	if (user.get().getInvalidCounter()==3l) {
    		return new ResponseEntity<>("User acoount is locked dues to too many inccorrect login attempt.Contact Administrator !!", HttpStatus.OK);
    	}
    		
    	try {
    		Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginDto.getUsernameOrEmail(), loginDto.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (BadCredentialsException e) {
			if (user.get().getInvalidCounter()==3) {
				return new ResponseEntity<>("Maximum login attempt Reached!!", HttpStatus.OK);
			}
			
			else if (user.get().getUsername()!=null)
			{
				user.get().setInvalidCounter(user.get().getInvalidCounter()+1);
			}
			
		}    
        return new ResponseEntity<>("User signed-in successfully!.", HttpStatus.OK);
    }
    
    @PostMapping("/unlockAccount")
    public ResponseEntity<?> unlockUser(@RequestBody LoginDto loginDto ,@RequestParam String user)
    {
	Optional<User> unblockUser = userRepository.findByUsernameOrEmail(loginDto.getUsernameOrEmail(), loginDto.getUsernameOrEmail());
	CustomUserDetails customUserDetails = new CustomUserDetails(unblockUser.get());	
	System.out.println(customUserDetails.getAuthorities());
	    if (!unblockUser.isPresent())
    	{
    		return new ResponseEntity<>("Username Not registered!!", HttpStatus.OK);
    	}
    	 else if (customUserDetails.getAuthorities().toString().indexOf("ADMIN")==-1) 
		  {
			  return new  ResponseEntity<>("Only admin can unlock the account!!", HttpStatus.OK); 		 
		  }
		 
    		
    	try {
    		Optional<User> unblockuser1 = userRepository.findByUsername (user);
    	    Calendar date3 = Calendar.getInstance();
		    	 if (!unblockuser1.isPresent())
		     	{
		     		return new ResponseEntity<>("Username Not registered!!", HttpStatus.OK);
		     	}
		    	 return new ResponseEntity<>("User unblocked successfully!.", HttpStatus.OK);
		    	 
    		}
    	catch(Exception e)
    	{
    		return new ResponseEntity<>("Exception occured", HttpStatus.OK);
    	}
  }
    
    
    @PostMapping("/user/sendotp")
    public ResponseEntity<?> verifyOtp(@RequestParam String user){
       	User currentUser = userRepository.findByUsername(user).get();
    	Calendar date2 = Calendar.getInstance();
    	if (!userRepository.existsByUsername(user)) {
    		return new ResponseEntity<>("You have not registered yet.", HttpStatus.NOT_FOUND);
    	} 
         
    	else if  (!userRepository.existsByEmail(currentUser.getEmail())) {
    		return new ResponseEntity<>("You have not registered yet.", HttpStatus.NOT_FOUND);
    	}
    	else if(currentUser.getVerified()) {
    		return new ResponseEntity<>("You are already verified, Kindly login!!", HttpStatus.NOT_FOUND);
    	}
    	  		
    		try 
    		{
    			otpService.generateOneTimePassword(currentUser);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (MessagingException e) {
				e.printStackTrace();
			}
    		return new ResponseEntity<>("OTP has been sent  successfully to your registered mail id !!", HttpStatus.OK);
    }
    
    @PostMapping("/user/verifyotp")
    public ResponseEntity<?> unlockUser(@RequestBody SignUpDto data)
    {
    	User unblockUser = userRepository.findByUsername(data.getUsername()).get();
    	if (unblockUser.getOneTimePassword()!= data.getOneTimePassword())
    	{
    		return new ResponseEntity<>(" otp mismatch ", HttpStatus.OK);
    	}
    	unblockUser.setVerified(true);
    	otpService.clearOTP(unblockUser);
    	return new ResponseEntity<>("otp verified ", HttpStatus.OK);
    }
    

	private void prepareUserObject(SignUpDto signUpDto, Calendar date, User user) {
		user.setName(signUpDto.getName());
        user.setUsername(signUpDto.getUsername());
        user.setEmail(signUpDto.getEmail());
        user.setPassword(passwordEncoder.encode(signUpDto.getPassword()));
        user.setAddress(signUpDto.getAddress());
        user.setMobileNo(signUpDto.getMobileNo());
        user.setVerified(false);
        user.setRegistartionDate(Calendar.getInstance());
        user.setInvalidCounter(0);
	}
   
}