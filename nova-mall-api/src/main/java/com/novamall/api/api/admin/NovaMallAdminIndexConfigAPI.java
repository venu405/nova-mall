/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本系统已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2021 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package com.novamall.api.api.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.novamall.api.api.admin.param.BatchIdParam;
import com.novamall.api.api.admin.param.IndexConfigAddParam;
import com.novamall.api.api.admin.param.IndexConfigEditParam;
import com.novamall.api.common.IndexConfigTypeEnum;
import com.novamall.api.common.ServiceResultEnum;
import com.novamall.api.config.annotation.TokenToAdminUser;
import com.novamall.api.entity.AdminUserToken;
import com.novamall.api.entity.IndexConfig;
import com.novamall.api.service.NovaMallIndexConfigService;
import com.novamall.api.util.BeanUtil;
import com.novamall.api.util.PageQueryUtil;
import com.novamall.api.util.Result;
import com.novamall.api.util.ResultGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 13
 * @qq交流群 796794009
 * @email 2449207463@qq.com
 * @link https://github.com/novamall
 */
@RestController
@Api(value = "v1", tags = "8-4.后台管理系统首页配置模块接口")
@RequestMapping("/manage-api/v1")
public class NovaMallAdminIndexConfigAPI {

    private static final Logger logger = LoggerFactory.getLogger(NovaMallAdminIndexConfigAPI.class);

    @Resource
    private NovaMallIndexConfigService novaMallIndexConfigService;

    /**
     * 列表
     */
    @RequestMapping(value = "/indexConfigs", method = RequestMethod.GET)
    @ApiOperation(value = "首页配置列表", notes = "首页配置列表")
    public Result list(@RequestParam(required = false) @ApiParam(value = "页码") Integer pageNumber,
                       @RequestParam(required = false) @ApiParam(value = "每页条数") Integer pageSize,
                       @RequestParam(required = false) @ApiParam(value = "1-搜索框热搜 2-搜索下拉框热搜 3-(首页)热销商品 4-(首页)新品上线 5-(首页)为你推荐") Integer configType, @TokenToAdminUser AdminUserToken adminUser) {
        logger.info("adminUser:{}", adminUser.toString());
        if (pageNumber == null || pageNumber < 1 || pageSize == null || pageSize < 10) {
            return ResultGenerator.genFailResult("分页参数异常！");
        }
        IndexConfigTypeEnum indexConfigTypeEnum = IndexConfigTypeEnum.getIndexConfigTypeEnumByType(configType);
        if (indexConfigTypeEnum.equals(IndexConfigTypeEnum.DEFAULT)) {
            return ResultGenerator.genFailResult("非法参数！");
        }
        Map params = new HashMap(8);
        params.put("page", pageNumber);
        params.put("limit", pageSize);
        params.put("configType", configType);
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        return ResultGenerator.genSuccessResult(novaMallIndexConfigService.getConfigsPage(pageUtil));
    }

    /**
     * 添加
     */
    @RequestMapping(value = "/indexConfigs", method = RequestMethod.POST)
    @ApiOperation(value = "新增首页配置项", notes = "新增首页配置项")
    public Result save(@RequestBody @Valid IndexConfigAddParam indexConfigAddParam, @TokenToAdminUser AdminUserToken adminUser) {
        logger.info("adminUser:{}", adminUser.toString());
        IndexConfig indexConfig = new IndexConfig();
        BeanUtil.copyProperties(indexConfigAddParam, indexConfig);
        String result = novaMallIndexConfigService.saveIndexConfig(indexConfig);
        if (ServiceResultEnum.SUCCESS.getResult().equals(result)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult(result);
        }
    }


    /**
     * 修改
     */
    @RequestMapping(value = "/indexConfigs", method = RequestMethod.PUT)
    @ApiOperation(value = "修改首页配置项", notes = "修改首页配置项")
    public Result update(@RequestBody @Valid IndexConfigEditParam indexConfigEditParam, @TokenToAdminUser AdminUserToken adminUser) {
        logger.info("adminUser:{}", adminUser.toString());
        IndexConfig indexConfig = new IndexConfig();
        BeanUtil.copyProperties(indexConfigEditParam, indexConfig);
        String result = novaMallIndexConfigService.updateIndexConfig(indexConfig);
        if (ServiceResultEnum.SUCCESS.getResult().equals(result)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult(result);
        }
    }

    /**
     * 详情
     */
    @RequestMapping(value = "/indexConfigs/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "获取单条首页配置项信息", notes = "根据id查询")
    public Result info(@PathVariable("id") Long id, @TokenToAdminUser AdminUserToken adminUser) {
        logger.info("adminUser:{}", adminUser.toString());
        IndexConfig config = novaMallIndexConfigService.getIndexConfigById(id);
        if (config == null) {
            return ResultGenerator.genFailResult("未查询到数据");
        }
        return ResultGenerator.genSuccessResult(config);
    }

    /**
     * 删除
     */
    @RequestMapping(value = "/indexConfigs", method = RequestMethod.DELETE)
    @ApiOperation(value = "批量删除首页配置项信息", notes = "批量删除首页配置项信息")
    public Result delete(@RequestBody BatchIdParam batchIdParam, @TokenToAdminUser AdminUserToken adminUser) {
        logger.info("adminUser:{}", adminUser.toString());
        if (batchIdParam == null || batchIdParam.getIds().length < 1) {
            return ResultGenerator.genFailResult("参数异常！");
        }
        if (novaMallIndexConfigService.deleteBatch(batchIdParam.getIds())) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("删除失败");
        }
    }

}