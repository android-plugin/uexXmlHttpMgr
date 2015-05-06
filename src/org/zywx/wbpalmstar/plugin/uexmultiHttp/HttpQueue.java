package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpQueue{
	
	private MyPool mExePool; 
	private ArrayList<HttpMsg> mRuningList;
	private ArrayList<HttpMsg> mAllList;
	private static HttpQueue instance;

	private HttpQueue(){
		mRuningList = new ArrayList<HttpMsg>(10);
		mAllList = new ArrayList<HttpMsg>(10);
		mExePool = new MyPool(3, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	public static HttpQueue get(){
		if(null == instance){
			instance = new HttpQueue();
		}
		return instance;
	}
	
	public void addTask(HttpMsg task){
		synchronized(mAllList){
			mAllList.add(task);
		}
	}
	
	public void executeTask(HttpMsg task){
		if(null != task){
			mRuningList.add(task);
			mExePool.execute(task);
		}
	}
	
	public boolean containsTask(int owerId, int opId){
		synchronized(mAllList){
			for(HttpMsg msg : mAllList){
				int oId = msg.getIdentity();
				int pId = msg.getOperateId();
				if(oId == owerId && pId == opId){
					return true;
				}
			}
			return false;
		}
	}
	
	public HttpMsg getTask(int id, int opId){
		synchronized(mAllList){
			for(HttpMsg msg : mAllList){
				int oId = msg.getIdentity();
				int pId = msg.getOperateId();
				if(oId == id && pId == opId){
					return msg;
				}
			}
			return null;
		}
	}
	
	public void removeTask(int owerId){
		synchronized(mAllList){
			for(Runnable runnable : mAllList){
				HttpMsg oneMsg = (HttpMsg)runnable;
				if(owerId != oneMsg.getIdentity()){
					continue;
				}
				if(oneMsg.inProgress()){
					oneMsg.forceClose();
				}
				mExePool.remove(runnable);
				mAllList.remove(runnable);
				mRuningList.remove(runnable);
			};
		}
	}
	
	public void removeTask(int owerId, int opId){
		synchronized(mAllList){
			for(HttpMsg msg : mAllList){
				int oId = msg.getIdentity();
				int pId = msg.getOperateId();
				if(oId == owerId && pId == opId){
					msg.forceClose();
					mExePool.remove(msg);
					mAllList.remove(msg);
					mRuningList.remove(msg);
					return;
				}
			}
		}
	}
	
	public void shutdown(){
		mExePool.shutdownAndAwaitTermination();
	}

	class MyPool extends ThreadPoolExecutor{

		public MyPool(int corePoolSize, int maximumPoolSize, 
				long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}
		
		@Override
		protected void beforeExecute(Thread t, Runnable task) {
			;
		}

		@Override
		protected void afterExecute(Runnable task, Throwable t) {
			synchronized(mRuningList){
				mRuningList.remove(task);
				mAllList.remove(task);
			}
		}

		@Override
		protected void terminated() {
			;
		}
		
		public void shutdownAndAwaitTermination(){
			synchronized(mRuningList){
				for(Runnable runnable : mRuningList){
					HttpMsg oneMsg = (HttpMsg)runnable;
					if(oneMsg.inProgress()){
						oneMsg.forceClose();
					}
					remove(runnable);
				}
				purge();
				mRuningList.clear();
				mAllList.clear();
			}
		}
	}
}
