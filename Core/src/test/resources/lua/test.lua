function printTable(table, initialContents) 
  if (initialContents) then
    print(initialContents)
  end
  
  for k, v in ipairs(table) do
    print(k..": "..v)
  end
end

printTable(_G, "Globals: ")
print("Version: ".._VERSION)

print(test.hello())
print(test.author)
