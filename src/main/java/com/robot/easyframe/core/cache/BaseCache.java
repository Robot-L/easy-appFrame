package com.robot.easyframe.core.cache;

import com.ai.aif.csf.zookeeper.client.curator.CuratorZkClient;
import com.ai.appframe2.bo.DataContainer;
import com.ai.appframe2.complex.cache.CacheFactory;
import com.ai.appframe2.complex.cache.CacheSource;
import com.ai.appframe2.complex.cache.ICache;
import com.ai.appframe2.complex.cache.impl.AbstractCache;
import com.ai.appframe2.complex.xml.XMLHelper;
import com.ai.appframe2.complex.xml.cfg.caches.Cache;
import com.ai.appframe2.complex.xml.cfg.caches.Caches;
import com.asiainfo.appframe.ext.exeframe.cache.load.v2.AppFrameCacheLoader;
import com.asiainfo.appframe.ext.exeframe.cache.zk.ZkClient;
import com.robot.easyframe.core.dao.BaseDao;
import com.robot.easyframe.util.Convert;
import com.robot.easyframe.util.DateUtil;
import com.robot.easyframe.util.ServiceUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.stream.Stream;

/**
 * 缓存基类
 *
 * 子类只需要继承BaseCache后指定泛型（某个Dao接口），即可默认缓存整表数据
 * 若需要自定义缓存内容，请重写getData()方法
 *
 * @author luozhan
 * @date 2019-10
 */
public class BaseCache<T extends BaseDao> extends AbstractCache {
    private Class<T> daoClass;
    private static Log log = LogFactory.getLog(BaseCache.class);

    @SuppressWarnings("unchecked")
    public BaseCache() {
        // 获取泛型参数T的class
        Type genType = this.getClass().getGenericSuperclass();
        try {
            Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
            daoClass = (Class<T>) params[0];
        } catch (Exception e) {
            log.error("BaseCache获取子类泛型时出现异常", e);
        }

    }

    @Override
    public HashMap getData() throws Exception {
        // 如果此方法报错"dao调用不在service中"，尝试使用下列注释代码，或者修改AIConfig.xml中的配置：CHECK_DAO_IN_SERVICE
        // if(!ServiceManager.getSession().isStartTransaction()) {
        //        ServiceManager.getSession().startTransaction();
        // }
        DataContainer[] data = ServiceUtil.get(daoClass).getAll();
        HashMap<Class, DataContainer[]> map = new HashMap<>(1);
        map.put(this.getClass(), data);
        return map;
    }

    /**
     * 刷新缓存
     *
     * @param cacheClass 缓存class
     * @throws Exception
     */
    public static void refresh(Class<? extends BaseCache> cacheClass) throws Exception {
        String cacheId = cacheClass.getName();
        Caches caches = XMLHelper.getInstance().getCaches();
        Cache cache = Stream.of(caches.getCaches()).filter(v -> v.getId().equals(cacheId)).findFirst().get();

        if (CacheSource.AICACHE == CacheSource.eval(cache.getSource())) {
            log.info("自动更新分布式缓存, cacheId='" + cacheId + "'");
            try {
                String newVersion = Convert.toStr(DateUtil.now());
                // 新加载一版缓存（实质上就是更新redis缓存并在zk对应缓存目录下新增一个版本节点）
                AppFrameCacheLoader.loadCache(cache, newVersion);
                // 更新zk文件夹信息，以触发各中心的本地缓存进行刷新
                CuratorZkClient zkClient = ZkClient.getInstance().getClient();
                String path = String.format("/AICACHE_LOAD/%s/%s", cache.getDataType(), cacheId);
                String data = String.format("{\"currentVersion\":%s,\"dbVersion\":%s}", newVersion, newVersion);
                zkClient.setData(path, data.getBytes());
            } catch (Throwable e) {
                log.error("自动更新缓存失败, cacheId='" + cacheId + "'", e);
            }
        } else {
            // 非分布式缓存，直接刷新即可
            ((ICache) CacheFactory._getCacheInstances().get(cacheClass)).refresh();
        }
    }

}
