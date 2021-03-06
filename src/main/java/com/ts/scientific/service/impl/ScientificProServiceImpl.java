package com.ts.scientific.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mchange.lang.IntegerUtils;
import com.ts.scientific.entity.*;
import com.ts.scientific.mapper.*;
import com.ts.scientific.service.ScientificProService;
import com.ts.scientific.util.RepResult;
import com.ts.scientific.util.WebUtils;
import com.ts.scientific.vo.FindProVO;
import com.ts.scientific.vo.ProPeopleVO;
import com.ts.scientific.vo.ProSeeVO;
import com.ts.scientific.vo.ProVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScientificProServiceImpl extends ServiceImpl<ScientificProMapper, ScientificPro> implements ScientificProService {

    @Autowired
    private ScientificProPeopleMapper scientificProPeopleMapper;
    @Autowired
    private ScientificProPeopleInfoMapper scientificProPeopleInfoMapper;
    @Autowired
    private ScientificProMapper scientificProMapper;
    @Autowired
    private ScientificInfoMapper scientificInfoMapper;
    @Autowired
    private ScientificInfoConfMapper scientificInfoConfMapper;
    @Autowired
    private ScientificInfoCentreMapper scientificInfoCentreMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AuthServiceImpl authServiceImpl;
    @Override
    public Object addPro(ProVO proVO, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        ScientificPro scientificPro = new ScientificPro();
        LocalDate now = LocalDate.now();
        if (now.isBefore(proVO.getStartTime())){
            proVO.setProStatus(1);
        }else {
            proVO.setProStatus(2);
        }
        proVO.setProNo(String.valueOf(System.currentTimeMillis()));
        proVO.setCreateTime(now);
        proVO.setCreateId(user.getUserId());
        proVO.setCreateName(user.getUserName());
        BeanUtils.copyProperties(proVO,scientificPro);
        this.save(scientificPro);
        List<ScientificProPeople> scientificProPeopleList = new ArrayList<>();
        ScientificProPeople proPeople  = null;
        int i = 1;
        for (Integer userId : proVO.getUserIds()) {
            proPeople = new ScientificProPeople();
            proPeople.setProId(scientificPro.getProId());
            proPeople.setCreateName(user.getUserName());
            proPeople.setUserId(userId);
            proPeople.setCreateTime(now);
            proPeople.setMaterialsStatus(3);
            proPeople.setRank(i++);
            scientificProPeopleList.add(proPeople);
        }
        if (scientificProPeopleList.size()>0) {
            addPeople(scientificProPeopleList, scientificProPeopleList.size()-1);
        }
        return RepResult.repResult(0,"申报成功",null);
    }

    @Override
    public Object getPro(FindProVO findProVO, HttpServletRequest request) {

        Map<String,List<String>> authCode = authServiceImpl.queryRoleAuth();
        User user = (User) WebUtils.getHttpSession().getAttribute("user");
//        String aCode=
//        if (authCode.get(user.getRoleId())!=null){
//            ///proPeopleVO.setFlag(1);
//              authCode.get(user.getRoleId()).contains("allPro");
//        }
        IPage<ScientificPro> page = new Page<>();
        page.setCurrent(findProVO.getCurrent());
        page.setSize(findProVO.getSize());
        List<ProSeeVO> proSeeVOS = new ArrayList<>();
        List<ScientificProPeople>  proPeopleIds =  scientificProPeopleMapper.selectList(new QueryWrapper<ScientificProPeople>().lambda()
                .eq(ScientificProPeople::getUserId,user.getUserId()));
        LambdaQueryWrapper<ScientificPro> qw = new LambdaQueryWrapper<>();
        if (findProVO.getProjectTypeId()!=null){
            qw.eq(ScientificPro::getProjectTypeId,findProVO.getProjectTypeId());
        }
        if (user.getRoleId() != 1) {
            qw.and(w -> w.eq(ScientificPro::getCreateId, user.getUserId()).or().in(proPeopleIds != null && proPeopleIds.size() > 0, ScientificPro::getProId, proPeopleIds.stream().map(ScientificProPeople::getProId).collect(Collectors.toList())));
            //qw.in(proPeopleIds != null && proPeopleIds.size() > 0, ScientificPro::getProId, proPeopleIds.stream().map(ScientificProPeople::getProId).collect(Collectors.toList()));
        }
        List<ScientificPro> scientificPros = scientificProMapper.selectPage(page,qw).getRecords();
        ProSeeVO proSeeVO = null;
        for (ScientificPro pro : scientificPros) {
            proSeeVO = new ProSeeVO();
            proSeeVO.setProId(pro.getProId());
            proSeeVO.setCalculateName(scientificInfoConfMapper.selectOne(new QueryWrapper<ScientificInfoConf>().lambda()
            .eq(ScientificInfoConf::getCalculateId,pro.getCalculateId())).getCalculateCondition());
            proSeeVO.setProType(scientificInfoMapper.selectOne(new QueryWrapper<ScientificInfo>().lambda()
                    .eq(ScientificInfo::getProjectTypeId,pro.getProjectTypeId())).getProjectTypeName());
            proSeeVO.setCreateName(pro.getCreateName());
            proSeeVO.setProNo(pro.getProNo());
            proSeeVO.setEnd(pro.getEndTime());
            proSeeVO.setStart(pro.getStartTime());
            proSeeVO.setProStatus(pro.getProStatus());
            List<ScientificProPeople> list = scientificProPeopleMapper.selectList(new QueryWrapper<ScientificProPeople>().lambda()
                    .eq(ScientificProPeople::getProId,pro.getProId()));
            List<ProPeopleVO> proPeopleVOS = new ArrayList<>();
            for (ScientificProPeople proPeople : list) {
                ProPeopleVO proPeopleVO = new ProPeopleVO();
                BeanUtils.copyProperties(proPeople,proPeopleVO);
                proPeopleVO.setUserName(userMapper.selectOne(new QueryWrapper<User>().lambda().eq(User::getUserId,proPeople.getUserId())).getUserName());
                proPeopleVOS.add(proPeopleVO);
            }
            proSeeVO.setProPeopleVOS(proPeopleVOS);
            proSeeVOS.add(proSeeVO);
        }
        return proSeeVOS;
    }


   public Object getCurrentProPeople(int current,int size,Integer proId){
        Map<String,List<String>> authCode = authServiceImpl.queryRoleAuth();
        User user = (User) WebUtils.getHttpSession().getAttribute("user");
        IPage<ScientificProPeople> iPage = new Page<>();
        iPage.setCurrent(current);
        iPage.setSize(size);
        ScientificPro scientificPro =  scientificProMapper.selectOne(new QueryWrapper<ScientificPro>().lambda()
                .eq(ScientificPro::getProId,proId));
        IPage<ScientificProPeople> proPeopleIPage = scientificProPeopleMapper.selectPage(iPage,new QueryWrapper<ScientificProPeople>().lambda()
                .eq(ScientificProPeople::getProId,proId));
        List<ScientificProPeople> list = proPeopleIPage.getRecords();
        List<ProPeopleVO> proPeopleVOS = new ArrayList<>();
        for (ScientificProPeople proPeople : list) {
            ProPeopleVO proPeopleVO = new ProPeopleVO();
            BeanUtils.copyProperties(proPeople,proPeopleVO);
            if (user.getRoleId()==1 || user.getUserId() == scientificPro.getCreateId() || authCode.get(user.getRoleId())!=null && authCode.get(user.getRoleId()).contains("deletePeople")){
                proPeopleVO.setFlag(1);
            }
            if (user.getRoleId()==1 || user.getUserId() == proPeople.getUserId()){
                proPeopleVO.setUserFlag(1);
            }
            if (user.getRoleId()==1 || user.getUserId() == scientificPro.getCreateId() || authCode.get(user.getRoleId())!=null && authCode.get(user.getRoleId()).contains("audit")){
                proPeopleVO.setAuditFlag(1);
            }
            proPeopleVO.setProStatus(scientificPro.getProStatus());
            proPeopleVO.setUserName(userMapper.selectOne(new QueryWrapper<User>().lambda().eq(User::getUserId,proPeople.getUserId())).getUserName());
            proPeopleVOS.add(proPeopleVO);
        }
        return RepResult.repResult(0,"",proPeopleVOS,Integer.valueOf(String.valueOf(proPeopleIPage.getTotal())));
    }

    public Object getAudit(String id,String bank){
        ScientificProPeople scientificProPeople = scientificProPeopleMapper.selectById(id);
        scientificProPeople.setMaterialsStatus(Integer.parseInt(bank));
        int i = scientificProPeopleMapper.updateById(scientificProPeople);
        List<ScientificProPeople> pro_id = scientificProPeopleMapper.selectList(new QueryWrapper<ScientificProPeople>().eq("pro_id", scientificProPeople.getProId()));
        boolean kens = true;
        for (ScientificProPeople proPeople : pro_id) {
            if(proPeople.getMaterialsStatus()!=1){
                kens = false;
            }
        }
        if(kens){
            ScientificPro scientificPro = scientificProMapper.selectById(scientificProPeople.getProId());
            ScientificPro scientificPro1 = scientificPro.setProStatus(2);
            int i1 = scientificProMapper.updateById(scientificPro1);
        }
        return RepResult.repResult(0,"修改成功",null);
    }

    @Override
    public Object addProMaterials(ScientificProPeopleInfo scientificProPeopleInfo, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        scientificProPeopleInfo.setCreateName(user.getUserName());
        scientificProPeopleInfo.setCreateTime(LocalDate.now());
        scientificProPeopleInfo.setUserId(user.getUserId());
        scientificProPeopleInfoMapper.insert(scientificProPeopleInfo);
        return RepResult.repResult(0,"上报成功",null);
    }

    @Override
    public Object updateMaterialsStatus(Integer status, Integer proPeopleId) {
        ScientificProPeople scientificProPeople = new ScientificProPeople();
        scientificProPeople.setMaterialsStatus(status);
        return scientificProPeopleMapper.update(scientificProPeople,new QueryWrapper<ScientificProPeople>().lambda()
                .eq(ScientificProPeople::getProPeopleId,proPeopleId));
    }

    /**
     * 使用递归算法循环添加
     */
    private Boolean addPeople(List<ScientificProPeople> proPeople,Integer index){
        scientificProPeopleMapper.insert(proPeople.get(index));
        index--;
        if (index == -1){
            return true;
        }
        return addPeople(proPeople,index);
    }

}
