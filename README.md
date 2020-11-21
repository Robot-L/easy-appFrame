# Easy-AppFrame
 【亚信专用】让appframe使用起来更方便更简洁，其中核心类BaseDao用于封装orm，以及其他若干基于BaseDao而优化的套件，涵盖了缓存、批量业务校验、批量字典翻译、表同步工具、集合工具、stream等，框架使用足够简单，所以取名为easy-appframe，为提高开发效率（体验）而生。


## 简介

BaseDao是基于appframe的dao层基类，对appframe底层进行了封装，面向对象设计，将原appframe提供的api进行的精简，设计了一套统一的API，并支持更丰富的sql功能。核心功能是省去了dao层的基础代码

使用BaseDao，dao层的代码相比原来能省50%（吧）。

## 前言

在之前，dao层的代码基本都是拼sql的天下，入参一个map或者10来个属性，方法体中充斥着大量的判空代码，每当有新的需求，可能就要新增方法，而每个方法都要取参数、判空拼sql，另外还要考虑日期边界啊，数据类型转换啊，等一系列繁琐的问题，此时内心OS：

1. **能不能通用一个查询方法，不要每次来了个新需求就要加一个查询方法？**
2. **能不能不要方法定义中10来个入参，或者在入参map中取10来个参数？**
3. **能不能不要每个属性都要判空拼sql，见到if语句就头晕**

于是BaseDao诞生了。


## 如何使用BaseDao?

假设为SIM卡表`BOResSimCardOriginBean`编写dao层代码，实现一个查询和修改的功能

1.定义接口`IResSimCardOriginDAO`，继承`BaseDao`，指定泛型`<BOResSimCardOriginBean>`

```java
public interface IResSimCardOriginDAO extends BaseDao<BOResSimCardOriginBean>{
//注意接口中不用写任何代码
}
```

2.定义实现类`ResSimCardOriginDAOImpl`，继承`BaseDaoImpl`，同样指定泛型

```java
public class IResSimCardOriginDAO extends BaseDaoImpl<BOResSimCardOriginBean> implements IResSimCardOriginDAO{
// 实现类中也不用写任何代码
}
```

好了，dao层的代码已经写完了，不用怀疑，来看下效果，测试代码如下：

```java
public static void main(String args[]) {

IResSimCardOriginDAO dao = ServiceUtil.get(IResSimCardOriginDAO.class);
DataContainer condition = new DataContainer();
// 1.设置查询条件：资源状态为2，3，4
condition.set("RES_STATE", Query.in(2,3,4));//Query类的作用在后面会讲
// 2.查询得到数据
BOResSimCardOriginBean[] simcards= dao.getBy(condition);
// 4.将查询的结果批量设置ResState为"5"
Arrays.asList(simcards).forEach(simcard -> simcard.setResState("5"));
// 保存入库
dao.update(simcards);
}
```

上例演示了将ResState为2、3、4的sim卡批量更新成5的代码。   
而dao层**没有任何方法代码**，而直接使用了继承自父类BaseDao的`getBy()`和`update()`两个方法进行查询和更新。

## API

目前BaseDao中共定义了16个方法（不包括重载方法），下列列出的是最常用的几个方法，完整的方法列表请看源码（文章末尾有源码路径）： 

```java
T[] getBy(DataContainer cond, Pagination page);//根据条件查询(支持分页)
T getById(Long id);//根据主键查询
T[] getByField(String fieldName, String... fieldValues);//根据指定属性对fieldValues中的值进行in查询
T[] getAll(Pagination page);//查询所有(支持分页)


int count(DataContainer cond);//根据查询条件计数
int countAll();//所有计数

DataContainer[] executeQuery(String sql, DataContainer cond);//执行自定义特殊sql查询(如分组查询、求Max等)
DataContainer[] executeUnionQuery(String baseSql, DataContainer cond, Pagination page)//执行连表查询，支持分页
  
int add(T... beans);//新增单个或多个bean
int delete(T... beans);//删除单个或多个bean
int update(T... beans);//更新单个或多个bean

int moveTo(BaseDao<K> destDao, List<T> beans);//将数据移动到另一个表（A表删除+B表新增），适合未用表移已用表、移历史表等操作
```

##### 说明

- 所有方法因为已被继承，都可以在实现类里直接使用，如果需要重写某方法，并且在该方法里调用原方法，可以用`super.方法名`调用
- `add()`方法不需要考虑主键的问题，绝大部分场景主键都是直接使用数据库序列（原框架需要自己塞入主键），同时也会判断对象中是否含有主键，有的话优先使用传入的。
- `add()`、`upadate()`和`delete()`都会自动设置对象的状态(isNew/isUpdate/isDelete)，而不像原框架增删改都是使用一个`save()`方法，不仅容易产生困惑，还要操心调用时如何给对象设置相应状态。
- 原Engine类增删改数据库时不会返回操作数量，BaseDao增加了这个返回值。
- `getBy()`方法入参page是分页对象，前台如果传了分页数据，可以通过` CsvUtil.getPageInfo()`从session中获取，另外如果只想查前n条数据，可以使用`new Pagination(n)`，如查前10条数据:

```java
dao.getAll(new Pagination(10))
```

继承了BaseDao后，Dao中不用写一句代码就能调用上述方法，所以，开发人员不用再写繁琐的Dao层代码，有更多的时间专注于实现复杂的业务。

## 两个关键类

### 1、DaoEngine-代替各个XXEngine

XXEngine代表每个bo对应的Engine类，使用工具新建每个bo的时候都会新建一个对应的XXEngine，其实这些Engine的代码高度相似，只有方法的入参类型或返回值类型是不同的，所以可以提取出相同代码，用泛型去控制类型的不同即可。除了提取相同的代码，DaoEngine中最核心的是**将查询条件DataContainer解析成sql的逻辑**。

DaoEngine出现后，所有的XXEngine类都不需要了。

### 2、Query-实现复杂查询条件

其实框架本身也是支持**面向对象查询**的，在XXEngine中有一个方法`getBeans(DataContainerInterface dc)`，可以使用`DataContainer`作为包装查询条件的容器，是面向对象的查询方式，不是比拼sql简洁的多吗，但为什么没有人使用这个方法呢，个人猜测是因为虽然这样的查询方式比拼接sql的方式简洁多了，但有个最大的弊端，就是功能单一，因为这种方式只能查询数据库表中某个属性等于某值这种场景（如查询表中no='1'和name='2'的结果），像sql中的between、like、in等都不支持，通用性太差了，所以无人问津，还是采用老式的拼接sql查询了。

因此，在BaseDao中，针对特殊sql操作符（`like`、`in`、`>`、`<`等）的场景，设计了一套传参和解析的逻辑，并提供了一系列的静态方法，放在`Query`类中，在DataContainer中设置条件的时候，使用相应方法即可，等调用查询方法到了dao层，经过DaoEngine类的解析，会将条件转换成sql语句再交给框架去执行(sql依旧用了绑定变量)。

- 各种场景的查询示例：

```java
DataContainer cond = new DataContainer();
cond.set("id", Query.in("111", "222", "333"));// id in '111','222','333'
cond.set("name", Query.like("%张三_"));// name like '%张三_'
cond.set("cardNo", Query.gt(143);// cardNo > 143
cond.set("age", Query.between(18,20));// age between 18 and 20
cond.set("done_date", Query.between("2019-01-01","2019-01-02"));// done_date between 2019-01-01(Date) and 2019-01-02(Date)
dao.getBy(cond);// 执行查询
```

有了Query类设置条件和DaoEngine的解析逻辑，就既能够面向对象的执行查询，又能保证方法的通用性了。

- #### Query类支持的查询方式

**api：**



```java
// not equal
ne(String v)

// In
in(String... v)
  
// Not In
notIn(String... v)
  
// Less Than( < )
lt(String v) 
lt(long v)
lt(Date v)

// Greater Than( > )
gt(String v)
gt(long v)
gt(Date v)

// Less Than or Equal( <= )
lte(String v) 
lte(long v)
lte(Date v)
  
// Greater Than or Equal( >= )
gte(String v) 
gte(long v)
gte(Date v)
  
// Between 
between(String v1, String v2) 
between(long v1, long v2) 
between(Date v1, Date v2) 
  
// Like
like(String) 
// 针对like查询条件还封装了4个常用方法
// 1.包含: like '%xx%'
include(String... include)
// 2.不包含: not like '%xx%'
exclude(String... exclude)
// 3.前缀匹配: like 'xx%'
prefix(String... prefix)
// 4.后缀匹配: like '%xx'
suffix(String... suffix)
```

- #### 用and()、or()为单个属性设置多个查询条件

看了上面的例子，好像都是一个属性一个条件，如果一个属性有多个条件怎么办，比如想查询id大于1并且不等于3的数据，

```java
// 错误示范
cond.set("id", Query.gt(1));
cond.set("id", Query.notIn(3));
```

这样是不行的，因为DataContainer用Map保存数据，set相同的key会被后面的覆盖掉。怎么办呢？见下文。

**Query类的两个连接条件的api**：

```java
and(String... cond);//以and连接多个条件（且）
or(String... cond);//以or连接多个条件（或）
```

**使用示例：**

```java
DataContainer cond = new DataContainer();
// 1.查询号码大于123并且不等于125
cond.set("id", Query.and(Query.gt('123'), Query.ne('125')));
// 2.查询号码前缀为8或者后缀为9
cond.set("id", Query.or(Query.prefix('8'), Query.suffix('9')));
```

- ### 排序

查询的结果想对查询结果中的某个属性进行排序，如何实现？

使用`Quyer.orderByAsc()`和`Quyer.orderByDesc()`提供升序和降序的功能。 

```java
// 1.根据号码升序查询
DataContainer cond = new DataContainer();
cond.set("id", Query.orderByAsc());
dao.getBy(cond);
```

需要对多个字段进行排序时，可以在调用方法时传入顺序参数，如`Quyer.orderByAsc(X)`和`Quyer.orderByDesc(X)`，数字X越小排序的顺序越靠前

```java
// 2.根据多个属性排序查询，先按照id升序排序，id相同的再age降序排序
DataContainer cond = new DataContainer();
cond.set("id", Query.orderByAsc(0));
cond.set("age", Query.orderByDesc(1));
dao.getBy(cond);
```

上面的例子中没有其他查询条件，若和其他查询条件一起使用时，只要用加号"+"连接即可，注意`Query.orderByAsc()`要放到"+"的右边，如下所示。

```java
// 3.和其他查询条件一起使用
DataContainer cond = new DataContainer();
cond.set("id", Query.between("88881111","88882222") + Query.orderByAsc());
cond.set("age", Query.lt(18) + Query.orderByDesc(1));
dao.getBy(cond);
```



##### 源码：

com.asiainfo.res.core.atom.instance.base.dao.interfaces.BaseDao.java

com.asiainfo.res.core.atom.instance.base.dao.impl.BaseDaoImpl.java

res/core/src/main/java/com/asiainfo/res/core/atom/instance/base/dao/frame/DaoEngine.java

res/core/src/main/java/com/asiainfo/res/core/atom/instance/base/dao/frame/Query.java



\------------------------------

罗战
[luozhan@asiainfo.com](mailto:luozhan@asiainfo.com)
13397323382
