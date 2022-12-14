package top.tangyh.lamp.authority.controller.auth;

import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.tangyh.basic.annotation.log.SysLog;
import top.tangyh.basic.annotation.security.PreAuth;
import top.tangyh.basic.base.R;
import top.tangyh.basic.base.controller.SuperCacheController;
import top.tangyh.basic.base.entity.SuperEntity;
import top.tangyh.basic.base.request.PageParams;
import top.tangyh.basic.context.ContextUtil;
import top.tangyh.basic.database.mybatis.conditions.query.LbqWrapper;
import top.tangyh.basic.database.mybatis.conditions.query.QueryWrap;
import top.tangyh.basic.interfaces.echo.EchoService;
import top.tangyh.basic.utils.ArgumentAssert;
import top.tangyh.lamp.authority.controller.poi.ExcelUserVerifyHandlerImpl;
import top.tangyh.lamp.authority.controller.poi.UserExcelDictHandlerImpl;
import top.tangyh.lamp.authority.dto.auth.UserExcelVO;
import top.tangyh.lamp.authority.dto.auth.UserPageQuery;
import top.tangyh.lamp.authority.dto.auth.UserRoleDTO;
import top.tangyh.lamp.authority.dto.auth.UserSaveDTO;
import top.tangyh.lamp.authority.dto.auth.UserUpdateAvatarDTO;
import top.tangyh.lamp.authority.dto.auth.UserUpdateBaseInfoDTO;
import top.tangyh.lamp.authority.dto.auth.UserUpdateDTO;
import top.tangyh.lamp.authority.dto.auth.UserUpdatePasswordDTO;
import top.tangyh.lamp.authority.entity.auth.User;
import top.tangyh.lamp.authority.entity.core.Org;
import top.tangyh.lamp.authority.service.auth.UserService;
import top.tangyh.lamp.authority.service.core.OrgService;
import top.tangyh.lamp.common.constant.BizConstant;
import top.tangyh.lamp.file.service.AppendixService;
import top.tangyh.lamp.model.entity.base.SysUser;
import top.tangyh.lamp.userinfo.service.UserHelperService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.groups.Default;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static top.tangyh.lamp.common.constant.SwaggerConstants.DATA_TYPE_LONG;
import static top.tangyh.lamp.common.constant.SwaggerConstants.DATA_TYPE_STRING;
import static top.tangyh.lamp.common.constant.SwaggerConstants.PARAM_TYPE_QUERY;


/**
 * <p>
 * ???????????????
 * ??????
 * </p>
 *
 * @author zuihou
 * @date 2019-07-22
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user")
@Api(value = "User", tags = "??????")
@PreAuth(replace = "authority:user:")
@RequiredArgsConstructor
public class UserController extends SuperCacheController<UserService, Long, User, UserPageQuery, UserSaveDTO, UserUpdateDTO> {
    private final OrgService orgService;
    private final EchoService echoService;
    private final AppendixService appendixService;
    private final ExcelUserVerifyHandlerImpl excelUserVerifyHandler;
    private final UserExcelDictHandlerImpl userExcelDictHandlerIImpl;
    private final UserHelperService userHelperService;
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", dataType = DATA_TYPE_LONG, paramType = PARAM_TYPE_QUERY),
            @ApiImplicitParam(name = "name", value = "??????", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    })
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????")
    @GetMapping("/check")
    public R<Boolean> check(@RequestParam(required = false) Long id, @RequestParam String name) {
        return success(baseService.check(id, name));
    }

    /**
     * ??????????????????
     *
     * @param data ??????DTO
     * @return ??????
     */
    @Override
    public R<User> handlerSave(UserSaveDTO data) {
        User user = BeanUtil.toBean(data, User.class);
        user.setReadonly(false);
        SysUser sysUser = userHelperService.getUserByIdCache(ContextUtil.getUserId());
        if (sysUser!= null) {
            data.setCreatedOrgId(sysUser.getOrgId());
        }
        baseService.saveUser(user);
        return success(user);
    }

    /**
     * ??????????????????
     *
     * @param ids ??????id
     * @return ????????????
     */
    @Override
    public R<Boolean> handlerDelete(List<Long> ids) {
        baseService.remove(ids);
        return success(true);
    }

    /**
     * ??????????????????
     *
     * @param data ??????
     * @return ??????
     */
    @Override
    public R<User> handlerUpdate(UserUpdateDTO data) {
        User user = BeanUtil.toBean(data, User.class);
        baseService.updateUser(user);
        return success(user);
    }

    /**
     * ??????
     *
     * @param data ??????????????????
     * @return ??????
     */
    @ApiOperation(value = "??????????????????")
    @PutMapping("/base")
    @SysLog(value = "'??????????????????:' + #data?.id", request = false)
    @PreAuth("hasAnyPermission('{}edit')")
    public R<User> updateBase(@RequestBody @Validated({SuperEntity.Update.class}) UserUpdateBaseInfoDTO data) {
        User user = BeanUtil.toBean(data, User.class);
        baseService.updateById(user);
        return success(user);
    }

    /**
     * ????????????
     *
     * @param data ??????????????????
     * @return ??????
     */
    @ApiOperation(value = "????????????", notes = "????????????")
    @PutMapping("/avatar")
    @SysLog("'????????????:' + #p0.id")
    public R<Boolean> avatar(@RequestBody @Validated(SuperEntity.Update.class) UserUpdateAvatarDTO data) {
        return success(baseService.updateAvatar(data));
    }

    /**
     * ????????????
     *
     * @param data ????????????
     * @return ????????????
     */
    @ApiOperation(value = "????????????", notes = "????????????")
    @PutMapping("/password")
    @SysLog("'????????????:' + #p0.id")
    public R<Boolean> updatePassword(@RequestBody @Validated(SuperEntity.Update.class) UserUpdatePasswordDTO data) {
        return success(baseService.updatePassword(data));
    }

    /**
     * ????????????
     *
     * @param data ????????????????????????
     * @return ????????????
     */
    @ApiOperation(value = "????????????", notes = "????????????")
    @PostMapping("/reset")
    @SysLog("'????????????:' + #data.id")
    public R<Boolean> reset(@RequestBody @Validated(SuperEntity.Update.class) UserUpdatePasswordDTO data) {
        baseService.reset(data);
        return success();
    }

    /**
     * ??????????????????????????????
     *
     * @param roleId  ??????id
     * @param keyword ???????????????
     * @return ????????????
     */
    @ApiOperation(value = "??????????????????????????????", notes = "??????????????????????????????")
    @GetMapping(value = "/role/{roleId}")
    public R<UserRoleDTO> findUserByRoleId(@PathVariable("roleId") Long roleId, @RequestParam(value = "keyword", required = false) String keyword) {
        List<User> list = baseService.findUserByRoleId(roleId, keyword);
        List<Long> idList = list.stream().mapToLong(User::getId).boxed().collect(Collectors.toList());
        return success(UserRoleDTO.builder().idList(idList).userList(list).build());
    }


    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @GetMapping("/find")
    @SysLog("??????????????????")
    public R<List<Long>> findAllUserId() {
        return success(baseService.findAllUserId());
    }

    @ApiOperation(value = "????????????????????????", notes = "????????????????????????")
    @GetMapping("/findAll")
    @SysLog("??????????????????")
    public R<List<User>> findAll() {
        List<User> res = baseService.list();
        res.forEach(obj -> obj.setPassword(null));
        return success(res);
    }

    @RequestMapping(value = "/findUserById", method = RequestMethod.GET)
    public R<List<User>> findUserById(@RequestParam(value = "ids") List<Long> ids) {
        return this.success(baseService.findUserById(ids));
    }

    @Override
    public Class<?> getExcelClass() {
        return UserExcelVO.class;
    }

    @Override
    public List<?> findExportList(PageParams<UserPageQuery> params) {
        return super.findExportList(params);
    }

    @Override
    public void enhanceExportParams(ExportParams ep) {
        ep.setDictHandler(userExcelDictHandlerIImpl);
    }

    @Override
    public R<Boolean> importExcel(@RequestParam("file") MultipartFile simpleFile, HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {
        ImportParams params = new ImportParams();
        params.setTitleRows(0);
        params.setHeadRows(1);
        params.setNeedVerify(true);
        params.setVerifyGroup(new Class[]{Default.class});
        // ?????????2???handler??????????????????????????????????????????????????????????????????N??????????????????????????????????????????????????????PR
        params.setVerifyHandler(excelUserVerifyHandler);
        params.setDictHandler(userExcelDictHandlerIImpl);

        ExcelImportResult<UserExcelVO> result = ExcelImportUtil.importExcelMore(simpleFile.getInputStream(), UserExcelVO.class, params);

        if (result.isVerifyFail()) {
            // ?????????????????????????????????????????????????????????????????? ??????????????????????????????????????????PR
            return R.validFail(result.getFailList().stream()
                    .map(item -> StrUtil.format("???{}???????????????: {}", item.getRowNum(), item.getErrorMsg()))
                    .collect(Collectors.joining("<br/>")));
        }

        List<UserExcelVO> list = result.getList();
        if (list.isEmpty()) {
            return this.validFail("????????????????????????");
        }

        Set<String> accounts = new HashSet<>();
        List<User> userList = list.stream().map(item -> {
            ArgumentAssert.notContain(accounts, item.getAccount(), "Excel????????????????????????: {}", item.getAccount());

            accounts.add(item.getAccount());
            User user = new User();
            BeanUtil.copyProperties(item, user);
            user.setSalt(RandomUtil.randomString(20));
            user.setPassword(SecureUtil.sha256(BizConstant.DEF_PASSWORD + user.getSalt()));
            return user;
        }).collect(Collectors.toList());

        baseService.saveBatch(userList);
        return this.success(true);
    }

    /**
     * ?????????????????????????????? ???????????????????????????
     *
     * @param params ????????????
     */
    @Override
    public IPage<User> query(PageParams<UserPageQuery> params) {
        IPage<User> page = params.buildPage(User.class);
        UserPageQuery userPage = params.getModel();

        QueryWrap<User> wrap = handlerWrapper(null, params);

        LbqWrapper<User> wrapper = wrap.lambda();
        if (userPage.getOrgId() != null && userPage.getOrgId() > 0) {
            List<Org> children = orgService.findChildren(Arrays.asList(userPage.getOrgId()));
            wrapper.in(User::getOrgId, children.stream().map(Org::getId).collect(Collectors.toList()));
        }
        wrapper.like(User::getName, userPage.getName())
                .like(User::getAccount, userPage.getAccount())
                .eq(User::getReadonly, false)
                .like(User::getEmail, userPage.getEmail())
                .like(User::getMobile, userPage.getMobile())
                .eq(User::getStationId, userPage.getStationId())
                .in(User::getPositionStatus, userPage.getPositionStatus())
                .in(User::getEducation, userPage.getEducation())
                .in(User::getNation, userPage.getNation())
                .in(User::getSex, userPage.getSex())
                .eq(User::getState, userPage.getState());

        if (StrUtil.equalsAny(userPage.getScope(), BizConstant.SCOPE_BIND, BizConstant.SCOPE_UN_BIND) && userPage.getRoleId() != null) {
            String sql = " select ur.employee_id from c_user_role ura where ura.user_id = s.id \n" +
                    "  and ura.role_id =   " + userPage.getRoleId();
            if (BizConstant.SCOPE_BIND.equals(userPage.getScope())) {
                wrapper.inSql(User::getId, sql);
            } else {
                wrapper.notInSql(User::getId, sql);
            }
        }

        baseService.findPage(page, wrapper);
        // ????????????
        echoService.action(page);

        page.getRecords().forEach(item -> {
            item.setPassword(null);
            item.setSalt(null);
        });

        appendixService.echoAppendix(page);

        return page;
    }

    @ApiOperation(value = "????????????????????????", notes = "????????????????????????")
    @PostMapping("/pageAll")
    @SysLog(value = "'??????????????????:???' + #params?.current + '???, ??????' + #params?.size + '???'", response = false)
    public R<IPage<User>> pageAll(@RequestBody @Validated PageParams<UserPageQuery> params) {
        IPage<User> page = params.buildPage(User.class);
        UserPageQuery userPage = params.getModel();

        QueryWrap<User> wrap = handlerWrapper(null, params);

        LbqWrapper<User> wrapper = wrap.lambda();
        if (userPage.getOrgId() != null && userPage.getOrgId() > 0) {
            List<Org> children = orgService.findChildren(Arrays.asList(userPage.getOrgId()));
            wrapper.in(User::getOrgId, children.stream().map(Org::getId).collect(Collectors.toList()));
        }
        wrapper.like(User::getName, userPage.getName())
                .like(User::getAccount, userPage.getAccount())
                .eq(User::getReadonly, false)
                .like(User::getEmail, userPage.getEmail())
                .like(User::getMobile, userPage.getMobile())
                .eq(User::getStationId, userPage.getStationId())
                .in(User::getPositionStatus, userPage.getPositionStatus())
                .in(User::getEducation, userPage.getEducation())
                .in(User::getNation, userPage.getNation())
                .in(User::getSex, userPage.getSex())
                .eq(User::getState, userPage.getState());

        if (StrUtil.equalsAny(userPage.getScope(), BizConstant.SCOPE_BIND, BizConstant.SCOPE_UN_BIND) && userPage.getRoleId() != null) {
            String sql = " select ur.user_id from c_user_role ura where ura.user_id = s.id \n" +
                    "  and ura.role_id =   " + userPage.getRoleId();
            if (BizConstant.SCOPE_BIND.equals(userPage.getScope())) {
                wrapper.inSql(User::getId, sql);
            } else {
                wrapper.notInSql(User::getId, sql);
            }
        }

        baseService.findPage(page, wrapper);
        // ????????????
        echoService.action(page);

        page.getRecords().forEach(item -> {
            item.setPassword(null);
            item.setSalt(null);
        });
        return R.success(page);
    }

}
