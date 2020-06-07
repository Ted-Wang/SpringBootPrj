package psn.ted.demo.springboot.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TradeStatus {
	@Id
	private Long tradeId;
	private String securityCode;
	private Integer quantity;
	
	// the following 4 field is not used currently.
	// They are just record, and may be used for business rule checking (potentially).
	private Long lastTradeVersion;
	private Long lastTransactionId;
	private String lastTradeActionType;
	private String lastTransactionType;
	
	public Long getTradeId() {
		return tradeId;
	}
	public void setTradeId(Long tradeId) {
		this.tradeId = tradeId;
	}
	public String getSecurityCode() {
		return securityCode;
	}
	public void setSecurityCode(String securityCode) {
		this.securityCode = securityCode;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	
	public Long getLastTradeVersion() {
		return lastTradeVersion;
	}
	public void setLastTradeVersion(Long lastTradeVersion) {
		this.lastTradeVersion = lastTradeVersion;
	}
	public Long getLastTransactionId() {
		return lastTransactionId;
	}
	public void setLastTransactionId(Long lastTransactionId) {
		this.lastTransactionId = lastTransactionId;
	}
	public String getLastTradeActionType() {
		return lastTradeActionType;
	}
	public void setLastTradeActionType(String lastTradeActionType) {
		this.lastTradeActionType = lastTradeActionType;
	}
	public String getLastTransactionType() {
		return lastTransactionType;
	}
	public void setLastTransactionType(String lastTransactionType) {
		this.lastTransactionType = lastTransactionType;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((securityCode == null) ? 0 : securityCode.hashCode());
		result = prime * result + ((quantity == null) ? 0 : quantity.hashCode());
		result = prime * result + ((tradeId == null) ? 0 : tradeId.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TradeStatus other = (TradeStatus) obj;
		if (securityCode == null) {
			if (other.securityCode != null)
				return false;
		} else if (!securityCode.equals(other.securityCode))
			return false;
		if (quantity == null) {
			if (other.quantity != null)
				return false;
		} else if (!quantity.equals(other.quantity))
			return false;
		if (tradeId == null) {
			if (other.tradeId != null)
				return false;
		} else if (!tradeId.equals(other.tradeId))
			return false;
		return true;
	}
	
}
