package main

import "fmt"

type Node struct {
	key  string
	val  int
	prev *Node
	next *Node
}

type LinkedList struct {
	head *Node
	tail *Node
}

type lru struct {
	list     *LinkedList
	mpp      map[string]*Node
	capacity int
}

func (lru *lru) put(key string, val int) {
	// Key already exists
	existingNode := lru.mpp[key]

	if existingNode != nil {
		existingNode.val = val
		lru.moveToFront(existingNode)
		return
	}

	// Cache is full
	if len(lru.mpp) == lru.capacity {
		lru.removeTail()
	}

	// Create new node
	node := &Node{
		key: key,
		val: val,
	}

	// Add to map
	lru.mpp[key] = node

	// Add to front
	lru.addToFront(node)
}

func (lru *lru) get(key string) (int, bool) {
	node := lru.mpp[key]

	if node == nil {
		return 0, false
	}

	// This node was recently used
	lru.moveToFront(node)

	return node.val, true
}

func (lru *lru) addToFront(node *Node) {
	node.prev = nil
	node.next = lru.list.head

	if lru.list.head != nil {
		lru.list.head.prev = node
	} else {
		// First node
		lru.list.tail = node
	}

	lru.list.head = node
}

func (lru *lru) remove(node *Node) {
	if node.prev != nil {
		node.prev.next = node.next
	} else {
		// Node is head
		lru.list.head = node.next
	}

	if node.next != nil {
		node.next.prev = node.prev
	} else {
		// Node is tail
		lru.list.tail = node.prev
	}

	node.prev = nil
	node.next = nil
}

func (lru *lru) moveToFront(node *Node) {
	// Already at front
	if lru.list.head == node {
		return
	}

	lru.remove(node)
	lru.addToFront(node)
}

func (lru *lru) removeTail() {
	if lru.list.tail == nil {
		return
	}

	node := lru.list.tail

	lru.remove(node)

	delete(lru.mpp, node.key)
}

func (lru *lru) print() {
	current := lru.list.head

	for current != nil {
		fmt.Printf("[%s:%d] ", current.key, current.val)
		current = current.next
	}

	fmt.Println()
}

func main() {
	cache := &lru{
		list:     &LinkedList{},
		mpp:      make(map[string]*Node),
		capacity: 3,
	}

	cache.put("1", 10)
	cache.put("2", 20)
	cache.put("3", 30)

	cache.print()
	// [3:30] [2:20] [1:10]

	cache.get("1")

	cache.print()
	// [1:10] [3:30] [2:20]

	cache.put("4", 40)

	cache.print()
	// [4:40] [1:10] [3:30]
	// 2 was removed because it was least recently used
}
