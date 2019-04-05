package com.cyh.netty.entity.webChat;

/**
 *  联系人
 * @author cyh
 *
 */
public class Contacts {

	private Integer id;
    private String name;
    private String userRegion; //用户ip所在省
    private String userCity; // 用户所在城市
    private Boolean isOnline; //是否在线
    
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUserRegion() {
		return userRegion;
	}
	public void setUserRegion(String userRegion) {
		this.userRegion = userRegion;
	}
	public String getUserCity() {
		return userCity;
	}
	public void setUserCity(String userCity) {
		this.userCity = userCity;
	}
	public Boolean getIsOnline() {
		return isOnline;
	}
	public void setIsOnline(Boolean isOnline) {
		this.isOnline = isOnline;
	}
}
