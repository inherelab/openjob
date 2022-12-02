package io.openjob.server.admin.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import io.openjob.common.constant.CommonConstant;
import io.openjob.common.util.CommonUtil;
import io.openjob.common.util.DateUtil;
import io.openjob.server.admin.autoconfigure.AdminUserProperties;
import io.openjob.server.admin.constant.AdminConstant;
import io.openjob.server.admin.request.user.AdminUserLoginRequest;
import io.openjob.server.admin.request.user.AdminUserLogoutRequest;
import io.openjob.server.admin.service.AdminLoginService;
import io.openjob.server.admin.vo.part.MenuItemVO;
import io.openjob.server.admin.vo.part.MenuMetaVO;
import io.openjob.server.admin.vo.part.PermItemVO;
import io.openjob.server.admin.vo.user.AdminUserLoginVO;
import io.openjob.server.admin.vo.user.AdminUserLogoutVO;
import io.openjob.server.common.exception.BusinessException;
import io.openjob.server.common.util.HmacUtil;
import io.openjob.server.repository.data.AdminMenuData;
import io.openjob.server.repository.data.AdminRuleData;
import io.openjob.server.repository.data.AdminUserData;
import io.openjob.server.repository.dto.AdminMenuDTO;
import io.openjob.server.repository.dto.AdminRuleDTO;
import io.openjob.server.repository.dto.AdminUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author inhere
 * @since 1.0.0
 */
@Service
public class AdminLoginServiceImpl implements AdminLoginService {

    private final AdminRuleData adminRuleData;

    private final AdminUserData adminUserData;

    private final AdminMenuData adminMenuData;

    private final AdminUserProperties userProperties;

    private final Cache<String, AdminUserLoginVO> loginCache;

    @Autowired
    public AdminLoginServiceImpl(
            AdminRuleData adminRuleData,
            AdminUserData adminUserData,
            AdminMenuData adminMenuData, AdminUserProperties userProperties,
            Cache<String, AdminUserLoginVO> loginCache
    ) {
        this.adminRuleData = adminRuleData;
        this.adminUserData = adminUserData;
        this.adminMenuData = adminMenuData;
        this.userProperties = userProperties;

        this.loginCache = loginCache;
    }

    @Override
    public AdminUserLoginVO login(AdminUserLoginRequest reqDTO) {
        AdminUserDTO entDTO = adminUserData.getByUsername(reqDTO.getUsername());
        checkLoginUser(entDTO, reqDTO.getPasswd());

        // build return vo
        AdminUserLoginVO vo = AdminUserLoginVO.builder()
                .id(entDTO.getId())
                .username(entDTO.getUsername())
                .nickname(entDTO.getNickname())
                .build();

        // query user perms and menus
        appendUserMenuAndPerms(vo, entDTO);

        return vo;
    }

    private void checkLoginUser(AdminUserDTO entDTO, String passwd) {
        if (Objects.isNull(entDTO)) {
            throw new BusinessException("input username is not exists");
        }

        if (CommonUtil.isTrue(entDTO.getDeleted())) {
            throw new BusinessException("input username is invalid");
        }

        if (!HmacUtil.verifyPasswd(entDTO.getPasswd(), passwd, userProperties.getPasswdSalt())) {
            throw new BusinessException("input user password is error");
        }

        if (CollectionUtils.isEmpty(entDTO.getRuleIds())) {
            throw new BusinessException("not set rule for user, please contact administrator");
        }
    }

    private String userSessionKey(String username) {
        String str = DateUtil.milliLongTime() + username;
        return HmacUtil.encrypt(str, "", HmacUtil.HMAC_MD5);
    }

    private void appendUserMenuAndPerms(AdminUserLoginVO vo, AdminUserDTO entDTO) {
        // query user rule and perms
        List<AdminRuleDTO> rules = adminRuleData.getByIds(entDTO.getRuleIds());
        if (CollectionUtils.isEmpty(rules)) {
            throw new BusinessException("login user rules not found");
        }

        boolean isAdmin = false;
        List<Long> menuIds = new ArrayList<>();

        // collect admin_menu.id list
        for (AdminRuleDTO ruleDto : rules) {
            if (CommonUtil.isTrue(ruleDto.getAdmin())) {
                isAdmin = true;
            }

            menuIds.addAll(ruleDto.getMenus());
            menuIds.addAll(ruleDto.getPerms());
        }

        vo.setSupperAdmin(isAdmin);

        List<PermItemVO> userPerms = new ArrayList<>();

        // query perms and menus
        List<AdminMenuDTO> dbMenuDtos = adminMenuData.getByIds(menuIds.stream().distinct().collect(Collectors.toList()));
        List<AdminMenuDTO> menuDtos = new ArrayList<>();

        for (AdminMenuDTO menuDto : dbMenuDtos) {
            if (AdminConstant.MENU_TYPE_PERM.equals(menuDto.getType())) {
                PermItemVO pItem = new PermItemVO();
                pItem.setPath(menuDto.getPath());
                pItem.setName(menuDto.getName());
                userPerms.add(pItem);
            } else if (AdminConstant.MENU_TYPE_MENU.equals(menuDto.getType())) {
                menuDtos.add(menuDto);
            }
        }

        vo.setPerms(userPerms);

        // format menus
        menuDtos.sort(Comparator.comparingInt(AdminMenuDTO::getSort));
        List<MenuItemVO> userMenus = formatTreeMenus(menuDtos);

        // storage session data
        String sessKey = userSessionKey(entDTO.getUsername());
        loginCache.put(sessKey, vo);

        vo.setMenus(userMenus);
    }

    private List<MenuItemVO> formatTreeMenus(List<AdminMenuDTO> dtoList) {

        List<String> sortProperties = new ArrayList<>();
        Map<Long, MenuItemVO> nodeList = new HashMap<>(dtoList.size());
        List<MenuItemVO> menuVos = new ArrayList<>();

        for (AdminMenuDTO dataRecord : dtoList) {
            MenuItemVO node = new MenuItemVO();
            node.setId(dataRecord.getId());
            node.setName(dataRecord.getName());
            node.setPath(dataRecord.getPath());
            node.setPid(dataRecord.getPid());

            // build meta info
            MenuMetaVO menuMeta = new MenuMetaVO();
            menuMeta.setIcon(dataRecord.getMeta().getIcon());
            menuMeta.setTitle(dataRecord.getMeta().getTitle());
            node.setMeta(menuMeta);

            // init sub menus
            List<MenuItemVO> temp = new ArrayList<>();
            node.setChildren(temp);

            nodeList.put(node.getId(), node);
        }

        for (AdminMenuDTO dataRecord : dtoList) {
            MenuItemVO vo = nodeList.get(dataRecord.getId());

            if (CommonConstant.LONG_ZERO.equals(dataRecord.getPid())) {
                menuVos.add(vo);
            } else {
                nodeList.get(dataRecord.getPid()).getChildren().add(vo);
            }
        }

        return menuVos;
    }

    @Override
    public AdminUserLogoutVO logout(AdminUserLogoutRequest reqDTO, String sessKey) {
        AdminUserLoginVO user = loginCache.getIfPresent(sessKey);
        if (Objects.isNull(user)) {
            throw new BusinessException("can not call logout");
        }

        // remove session data
        loginCache.invalidate(sessKey);

        return new AdminUserLogoutVO();
    }

}
