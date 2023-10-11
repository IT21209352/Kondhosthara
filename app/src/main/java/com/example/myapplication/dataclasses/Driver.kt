package com.example.myapplication.dataclasses

data class Driver(
      val ownerUid:String? = null,
      val name:String? = null,
      val email:String? = null,
      val phone:String? = null,
      val address:String? = null,
      val nic:String? = null,
      val type:String? = null,
      val busID : String ?= null,
      val drvHrs : String ?= null,
      val distTravel : String ?= null,
      val uid : String ?= null,
      val status : String ? =null,
      val isJourneyStarted : Boolean ?= false
    )