###
  Copyright (c) 2002-2012 "Neo Technology,"
  Network Engine for Objects in Lund AB [http://neotechnology.com]

  This file is part of Neo4j.

  Neo4j is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
###
define(
  ['lib/backbone', 'lib/has'], 
  () ->

    class LocalStorageStoringStrategy
      
      store : (key, obj) ->
        localStorage.setItem(key, JSON.stringify(obj))

      fetch : (key, defaults={}) ->
        stored = localStorage.getItem(key)
        if stored != null then JSON.parse(stored) else defaults

    class InMemoryStoringStrategy
      
      constructor : () ->
        @storage = {}

      store : (key, obj) ->
        @storage[key] = obj

      fetch : (key, defaults={}) ->
        if @storage[key]? then @storage[key] else @defaults
    
    class LocallyStoredModel extends Backbone.Model

      initialize : () ->
        if has("native-localstorage")
          @storingStrategy = new LocalStorageStoringStrategy()
        else
          @storingStrategy = new InMemoryStoringStrategy()
        
      getStorageKey : () ->
        # Subclasses must override this
        "default-locally-stored-model"

      fetch : () ->
        @clear({silent:true})
        @set(@storingStrategy.fetch(@getStorageKey(), @defaults))

      save : () ->
        @storingStrategy.store(@getStorageKey(), @toJSON())

)
