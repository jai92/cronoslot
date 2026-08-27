package com.cronoslot

data class Pilot(val id:Long,val name:String,val surname:String,val remotes:String,val photo:String?)
data class Car(val id:Long,val brand:String,val model:String,val chassis:String,val frontTyre:String,val rearTyre:String,val braid:String,val notes:String,val photo:String?)
data class Track(val id:Long,val name:String,val length:Double,val notes:String,val photo:String?)
data class Session(val id:Long,val pilotId:Long,val carId:Long,val trackId:Long,val remote:String,val started:Long,val finished:Long?,val notes:String,val laps:Int,val best:Double,val average:Double,val distance:Double)
data class Lap(val id:Long,val sessionId:Long,val number:Int,val seconds:Double)
