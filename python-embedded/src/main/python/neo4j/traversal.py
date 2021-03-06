# Copyright (c) 2002-2011 "Neo Technology,"
# Network Engine for Objects in Lund AB [http://neotechnology.com]
#
# This file is part of Neo4j.
#
# Neo4j is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

from _backend import extends, implements, rel_type,\
    TraversalPath, TraverserImpl, Traversal,\
    TraversalDescriptionImpl, strings,\
    Evaluation, Evaluator, Uniqueness
        
from util import PythonicIterator
        
from core import Direction, DirectionalType

class DynamicEvaluator(implements(Evaluator)):
    def __init__(self, eval_method):
        self._eval_method = eval_method

    def evaluate(self, path):
        return self._eval_method(path)

#
# Pythonification of the traversal API
#

class TraversalPath(extends(TraversalPath)):

    @property
    def start(self): return self.startNode()
    
    @property
    def end(self):   return self.endNode()
    
    @property
    def last_relationship(self): return self.lastRelationship()
    
    @property
    def nodes(self): return self._super__nodes()
    
    @property
    def relationships(self): return self._super__relationships()
    
    def __repr__(self): return self.toString()
    
    def __len__(self):  return self.length()
    
    def __iter__(self):
        it = self.iterator()
        while it.hasNext():
            yield it.next() 

      
class TraversalDescriptionImpl(extends(TraversalDescriptionImpl)):
    
    def relationships(self, reltype, direction=Direction.ANY):
    
        if type(reltype) in strings:
            reltype = rel_type(reltype)
        elif isinstance(reltype, DirectionalType):
            direction = reltype.dir
            reltype = reltype.type
        return self._super__relationships(reltype, direction)
        
    def evaluator(self, ev):
        if hasattr(ev, '__call__'):
            ev = DynamicEvaluator(ev)
        return self._super__evaluator(ev)
        
class TraverserImpl(extends(TraverserImpl)):
    
    def __iter__(self):
        it = self.iterator()
        while it.hasNext():
            yield it.next()
            
    @property
    def last_relationship(self): return self.lastRelationship()
    
    @property
    def nodes(self): return self._super__nodes()
    
    @property
    def relationships(self): return self._super__relationships()
