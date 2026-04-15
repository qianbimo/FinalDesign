package com.finaldesign.lungnodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finaldesign.lungnodule.entity.DoctorProfile;
import com.finaldesign.lungnodule.dto.LoginRequest;
import com.finaldesign.lungnodule.dto.RegisterRequest;
import com.finaldesign.lungnodule.entity.PatientProfile;
import com.finaldesign.lungnodule.entity.SysUser;
import com.finaldesign.lungnodule.exception.BusinessException;
import com.finaldesign.lungnodule.mapper.DoctorProfileMapper;
import com.finaldesign.lungnodule.mapper.PatientProfileMapper;
import com.finaldesign.lungnodule.mapper.SysUserMapper;
import com.finaldesign.lungnodule.security.JwtTokenProvider;
import com.finaldesign.lungnodule.security.LoginUser;
import com.finaldesign.lungnodule.service.AuthService;
import com.finaldesign.lungnodule.utils.NoGenerator;
import com.finaldesign.lungnodule.vo.LoginResponseVO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final DoctorProfileMapper doctorProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           PatientProfileMapper patientProfileMapper,
                           DoctorProfileMapper doctorProfileMapper,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider) {
        this.sysUserMapper = sysUserMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.doctorProfileMapper = doctorProfileMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        if (!"PATIENT".equals(request.getRole()) && !"DOCTOR".equals(request.getRole())) {
            throw new BusinessException(400, "Only PATIENT or DOCTOR can register");
        }

        SysUser existed = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (existed != null) {
            throw new BusinessException(400, "Username already exists");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        sysUserMapper.insert(user);

        if ("PATIENT".equals(request.getRole())) {
            PatientProfile patientProfile = new PatientProfile();
            patientProfile.setUserId(user.getId());
            patientProfile.setMedicalRecordNo(NoGenerator.medicalRecordNo());
            patientProfileMapper.insert(patientProfile);
        } else if ("DOCTOR".equals(request.getRole())) {
            DoctorProfile doctorProfile = new DoctorProfile();
            doctorProfile.setUserId(user.getId());
            doctorProfileMapper.insert(doctorProfile);
        }

        return user.getId();
    }

    @Override
    public LoginResponseVO login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(loginUser);

        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        return new LoginResponseVO(token, user.getRole(), user.getId(), user.getRealName());
    }
}
