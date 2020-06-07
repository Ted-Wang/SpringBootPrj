package psn.ted.demo.springboot.rest;

import java.util.List;

import psn.ted.demo.springboot.entity.EquityPosition;

public class EquityPostionREST {
	private long id;
	private List<EquityPosition> equityPositionList;
	private String errorMsg;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<EquityPosition> getEquityPositionList() {
		return equityPositionList;
	}

	public void setEquityPositionList(List<EquityPosition> equityPositionList) {
		this.equityPositionList = equityPositionList;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((equityPositionList == null) ? 0 : equityPositionList.hashCode());
		result = prime * result + ((errorMsg == null) ? 0 : errorMsg.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
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
		EquityPostionREST other = (EquityPostionREST) obj;
		if (equityPositionList == null) {
			if (other.equityPositionList != null)
				return false;
		} else if (!equityPositionList.equals(other.equityPositionList))
			return false;
		if (errorMsg == null) {
			if (other.errorMsg != null)
				return false;
		} else if (!errorMsg.equals(other.errorMsg))
			return false;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EquityPostionREST [id=" + id + ", equityPositionList=" + equityPositionList + ", errorMsg=" + errorMsg
				+ "]";
	}

}
